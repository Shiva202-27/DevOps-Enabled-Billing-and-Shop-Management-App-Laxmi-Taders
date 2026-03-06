package com.shiv.shop;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.ContentResolver;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private TextView textTodaySales;

    private DrawerLayout drawer;
    RecyclerView productRecyclerView;
    DBHelper dbHelper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ Backup Button Logic
        Button btnBackup = findViewById(R.id.btnBackupDb);
        btnBackup.setOnClickListener(v -> backupDatabaseToDownloads());
        findViewById(R.id.btnExportCsv).setOnClickListener(v -> exportBillingHistoryToCsv());

        // ✅ Setup Drawer
        drawer = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        // ✅ Handle back button with drawer
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        // ✅ Setup NavigationView
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // ✅ Initialize DB and display today's sales and recent bills
        dbHelper = new DBHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Toast.makeText(this, "Database created successfully!", Toast.LENGTH_SHORT).show();

        RecyclerView recyclerView = findViewById(R.id.listRecentBills);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<RecentBill> recentBillList = dbHelper.getRecentBillObjects();
        RecentBillAdapter adapter = new RecentBillAdapter(recentBillList);
        recyclerView.setAdapter(adapter);

// Update Today's Sales
        textTodaySales = findViewById(R.id.textTodaySales);
        double totalToday = dbHelper.getTodaySalesTotal();
        textTodaySales.setText("Today's Sales: ₹"+totalToday);



        // ✅ New Purchase
        Button newPurchaseBtn = findViewById(R.id.newPurchaseButton);

        newPurchaseBtn.setOnClickListener(v -> {
            // Load and start the animation
            Animation anim = AnimationUtils.loadAnimation(MainActivity.this, R.anim.button_click);
            v.startAnimation(anim);

            // Wait until animation finishes before navigating
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    Intent intent = new Intent(MainActivity.this, NewPurchaseActivity.class);
                    startActivity(intent);
                    // Optional: Add transition effect
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        });

        Button btnRestore = findViewById(R.id.btnRestoreDb);
        btnRestore.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            restoreFilePicker.launch(Intent.createChooser(intent, "Select .db backup file"));
        });
        dbHelper.logLast5BillDates();
        PeriodicWorkRequest backupWorkRequest = new PeriodicWorkRequest.Builder(BackupWorker.class, 1, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_backup",
                ExistingPeriodicWorkPolicy.KEEP,
                backupWorkRequest);
    }

    private void restoreDatabaseFromUri(Uri uri) {
        try {
            File dbFile = getDatabasePath("ShopApp.db");

            ContentResolver resolver = getContentResolver();
            FileInputStream fis = (FileInputStream) resolver.openInputStream(uri);
            FileOutputStream fos = new FileOutputStream(dbFile, false);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fis.close();
            fos.close();

            Toast.makeText(this, "✅ Restore successful. Restart app.", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Restore failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private final ActivityResultLauncher<Intent> restoreFilePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedFileUri = result.getData().getData();
                    restoreDatabaseFromUri(selectedFileUri);
                }
            });

    // ✅ Backup to Downloads folder
    private void backupDatabaseToDownloads() {
        try {
            File dbFile = getDatabasePath("ShopApp.db");
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) downloadsDir.mkdirs();

            File backupFile = new File(downloadsDir, "ShopApp_backup.db");

            FileInputStream fis = new FileInputStream(dbFile);
            FileOutputStream fos = new FileOutputStream(backupFile);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fis.close();
            fos.close();

            Toast.makeText(this, "✅ Backup saved to Downloads!", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void exportBillingHistoryToCsv() {
        try {
            File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!exportDir.exists()) exportDir.mkdirs();

            File file = new File(exportDir, "ShopApp_Bills.csv");
            FileWriter writer = new FileWriter(file);

            // Header
            writer.append("Bill No,Date,Customer,Phone,Total\n");

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM bills ORDER BY date DESC", null);

            if (cursor.moveToFirst()) {
                do {
                    String billNo = cursor.getString(cursor.getColumnIndexOrThrow("bill_number"));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                    String name = cursor.getString(cursor.getColumnIndexOrThrow("customer_name"));
                    String phone = cursor.getString(cursor.getColumnIndexOrThrow("phone"));
                    String total = cursor.getString(cursor.getColumnIndexOrThrow("total"));

                    writer.append(billNo).append(",")
                            .append(date).append(",")
                            .append(name).append(",")
                            .append(phone).append(",")
                            .append(total).append("\n");

                } while (cursor.moveToNext());
            }

            cursor.close();
            writer.flush();
            writer.close();

            // ✅ Make it visible in file manager
            MediaScannerConnection.scanFile(this,
                    new String[]{file.getAbsolutePath()},
                    new String[]{"text/csv"},
                    null);

            Toast.makeText(this, "✅ CSV exported: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    // ✅ Drawer menu
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_add_item) {
            startActivity(new Intent(this, AddItemActivity.class));
        } else if (id == R.id.nav_edit_item) {
            startActivity(new Intent(this, EditItemActivity.class));
        } else if (id == R.id.nav_view_history) {
            startActivity(new Intent(this, BillHistoryActivity.class));
        } else if (id == R.id.nav_manage_products) {
            startActivity(new Intent(this, ManageProductsActivity.class));
        }
        else if (id == R.id.nav_sales_summary) {
            startActivity(new Intent(this, SalesSummaryActivity.class));
                }
        else if (id== R.id.nav_duePayment){
            startActivity(new Intent(this, DuePaymentActivity.class));
        }



        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }

        return true;
    }
    private void animateSalesUpdate(TextView textView, double oldValue, double newValue) {
        ValueAnimator animator = ValueAnimator.ofFloat((float) oldValue, (float) newValue);
        animator.setDuration(1000); // 1 second animation
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            textView.setText(String.format(Locale.getDefault(), "Today's Sales: ₹%.2f", animatedValue));
        });
        animator.start();
    }
    private void animateSalesCounter(TextView textView, double from, double to) {
        ValueAnimator animator = ValueAnimator.ofFloat((float) from, (float) to);
        animator.setDuration(2000); // 1.5 seconds
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            textView.setText("₹" + String.format(Locale.getDefault(), "%.2f", animatedValue));
        });
        animator.start();
    }


    @Override
    protected void onResume() {
        super.onResume();

        dbHelper = new DBHelper(this);

        RecyclerView recyclerView = findViewById(R.id.listRecentBills);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ArrayList<RecentBill> recentBillList = dbHelper.getRecentBillObjects();
        RecentBillAdapter adapter = new RecentBillAdapter(recentBillList);
        recyclerView.setAdapter(adapter);

        // Animate Today's Sales from 0 → totalToday
        TextView textTodaySales = findViewById(R.id.textTodaySales);
        double totalToday = dbHelper.getTodaySalesTotal();
        animateSalesCounter(textTodaySales, 0, totalToday);  // 🔥 Always animate from 0

        // Update Bill Count
        TextView textTodayBillCount = findViewById(R.id.textTodayBillCount);
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int billCount = dbHelper.getBillCountByDate(todayDate);
        textTodayBillCount.setText("Bills: " + billCount);

        // Update Avg Sale/Customer
        TextView textAvgSale = findViewById(R.id.textAvgSale);
        double avgSale = dbHelper.getAvgSalePerCustomer(todayDate);
        String formattedAvg = String.format(Locale.getDefault(), "%.2f", avgSale);
        textAvgSale.setText("Avg. Sale/Customer: ₹" + formattedAvg);

        // Update Date
        TextView textDate = findViewById(R.id.textTodayDate);
        textDate.setText("Date: " + todayDate);


        String pdfPath = getIntent().getStringExtra("pdfPath");
        if (pdfPath != null) {
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                Uri pdfUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".provider",
                        pdfFile
                );

                Intent viewPdf = new Intent(Intent.ACTION_VIEW);
                viewPdf.setDataAndType(pdfUri, "application/pdf");
                viewPdf.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(viewPdf);

                // Prevent reopening on next resume
                getIntent().removeExtra("pdfPath");
            }
        }

    }

}