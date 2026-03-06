package com.shiv.shop;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BackupWorker extends Worker {

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("BackupWorker", "Daily backup started");

        boolean dbOk = backupDatabase();
        boolean dueOk = backupFolder("due_receipts");
        boolean billOk = backupFolder("bill_receipts");

        if (dbOk && dueOk && billOk) {
            Log.d("BackupWorker", "Daily backup completed successfully");
            return Result.success();
        } else {
            Log.e("BackupWorker", "Daily backup failed");
            return Result.failure();
        }
    }

    // Backup Database
    private boolean backupDatabase() {
        try {
            File dbFile = getApplicationContext().getDatabasePath(DBHelper.DB_NAME);
            File backupDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "ShopAppBackup");
            if (!backupDir.exists()) backupDir.mkdirs();

            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            File backupFile = new File(backupDir, "ShopApp_backup_" + date + ".db");

            copyFile(dbFile, backupFile);
            Log.d("BackupWorker", "Database backup saved: " + backupFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.e("BackupWorker", "Database backup failed", e);
            return false;
        }
    }

    // Backup Any Folder (e.g. due_receipts, bill_receipts)
    private boolean backupFolder(String folderName) {
        try {
            File srcDir = new File(getApplicationContext().getExternalFilesDir(null), folderName);
            if (!srcDir.exists()) {
                Log.w("BackupWorker", "Source folder not found: " + folderName);
                return true; // Not an error if folder doesn't exist yet
            }

            File destDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOCUMENTS), "ShopAppBackup/" + folderName);
            if (!destDir.exists()) destDir.mkdirs();

            File[] files = srcDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    File destFile = new File(destDir, file.getName());
                    copyFile(file, destFile);
                    Log.d("BackupWorker", "Copied " + file.getName() + " to " + destDir.getAbsolutePath());
                }
            }
            return true;
        } catch (Exception e) {
            Log.e("BackupWorker", "Backup failed for folder: " + folderName, e);
            return false;
        }
    }

    // File Copy Utility
    private void copyFile(File sourceFile, File destFile) throws IOException {
        if (!sourceFile.exists()) return;
        try (FileChannel src = new FileInputStream(sourceFile).getChannel();
             FileChannel dst = new FileOutputStream(destFile).getChannel()) {
            dst.transferFrom(src, 0, src.size());
        }
    }
}
