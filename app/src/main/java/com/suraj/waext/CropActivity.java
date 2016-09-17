package com.suraj.waext;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.soundcloud.android.crop.Crop;

import java.io.File;


public class CropActivity extends AppCompatActivity {

    private String fileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop);


        fileName = getIntent().getStringExtra("contactNumber");


        if (ContextCompat.checkSelfPermission(CropActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(CropActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);

        }

        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");

        startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.select_picture)), 0X1);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == 0X1 && resultCode == RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }
        CropActivity.this.finish();
    }

    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(Environment.getExternalStorageDirectory().toString() + "/WhatsApp/Media/WallPaper/", fileName + ".jpg"));
        Crop.of(source, destination).withAspect(9, 16).start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
            //resultView.setImageURI(Crop.getOutput(result));

        } else if (resultCode == Crop.RESULT_ERROR) {
            //Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
