package com.shiv.shop;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;

public class BillHistoryActivity extends AppCompatActivity {

    LinearLayout billListLayout;
    TextView textFromDate, textToDate;
    Button btnSearchBills, btnExportExcel;
    String fromDate = "", toDate = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_history);

        billListLayout = findViewById(R.id.billListLayout);
        textFromDate = findViewById(R.id.textFromDate);
        textToDate = findViewById(R.id.textToDate);
        btnSearchBills = findViewById(R.id.btnSearchBills);
        btnExportExcel = findViewById(R.id.btnExportExcel);  // Make sure this button exists in XML

        textFromDate.setOnClickListener(v -> pickDate(true));
        textToDate.setOnClickListener(v -> pickDate(false));

        btnSearchBills.setOnClickListener(v -> {
            if (fromDate.isEmpty() || toDate.isEmpty()) {
                Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show();
                return;
            }

            ArrayList<String> filtered = new DBHelper(this).getBillsBetween(fromDate, toDate);
            billListLayout.removeAllViews();

            if (filtered.isEmpty()) {
                Toast.makeText(this, "No bills in selected range", Toast.LENGTH_SHORT).show();
            }

            for (String bill : filtered) {
                TextView textView = new TextView(this);
                textView.setText(bill);
                textView.setTextSize(16);
                textView.setPadding(16, 16, 16, 16);
                billListLayout.addView(textView);
            }
        });

        btnExportExcel.setOnClickListener(v -> {
            File file = new DBHelper(this).exportBillsToExcel(this);
            if (file != null) {
                Toast.makeText(this, "Exported: " + file.getName(), Toast.LENGTH_SHORT).show();
                shareFile(file);
            } else {
                Toast.makeText(this, "Failed to export Excel", Toast.LENGTH_SHORT).show();
            }
        });

        loadAllBills(); // Show all on start
    }

    private void pickDate(boolean isFrom) {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, (view, y, m, d) -> {
            String selected = String.format("%04d-%02d-%02d", y, m + 1, d);
            if (isFrom) {
                fromDate = selected;
                textFromDate.setText("From: " + selected);
            } else {
                toDate = selected;
                textToDate.setText("To: " + selected);
            }
        }, year, month, day).show();
    }

    private void loadAllBills() {
        File dir = new File(getExternalFilesDir(null), "bills");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                for (File file : files) {
                    if (file.getName().endsWith(".pdf")) {
                        TextView textView = new TextView(this);
                        textView.setText(file.getName());
                        textView.setTextSize(16);
                        textView.setPadding(16, 16, 16, 16);
                        textView.setOnClickListener(v -> openPdf(file));
                        billListLayout.addView(textView);
                    }
                }
            } else {
                Toast.makeText(this, "No bills found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openPdf(File pdfFile) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdfFile);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open PDF", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void shareFile(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/vnd.ms-excel");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Excel file via"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
