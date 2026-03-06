package com.shiv.shop;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;

public class ViewPdfActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String path = getIntent().getStringExtra("path");
        File file = new File(path);

        if (!file.exists()) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Uri uri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No PDF Viewer found", Toast.LENGTH_SHORT).show();
        }
        finish(); // close activity after opening
    }
}
