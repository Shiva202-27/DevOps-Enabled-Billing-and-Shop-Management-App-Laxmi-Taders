package com.shiv.shop;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Apache POI imports
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFRow;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DB_NAME = "ShopApp.db";
    public static final int DB_VERSION = 5;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS bills (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "bill_number TEXT," +
                "customer_name TEXT," +
                "phone TEXT," +
                "date TEXT," +
                "total REAL)");

        db.execSQL("CREATE TABLE IF NOT EXISTS products (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "name_marathi TEXT, " +   // ✅ New column
                "category TEXT NOT NULL, " +
                "unit TEXT, " +
                "price REAL NOT NULL)");


        db.execSQL("CREATE TABLE customers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "phone TEXT)");


        db.execSQL("CREATE TABLE IF NOT EXISTS bill_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "bill_number TEXT, " +
                "item_name TEXT, " +
                "quantity REAL, " +
                "rate REAL, " +
                "total REAL, " +
                "FOREIGN KEY(bill_number) REFERENCES bills(bill_number))");
        Log.d("DBhelper", "Tables created successfully");


        db.execSQL("CREATE TABLE IF NOT EXISTS temp_cart (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_name TEXT, " +
                "price REAL, " +
                "unit TEXT, " + "" +
                "quantity REAL, " +
                "session_id TEXT DEFAULT 'BILL1')");

        db.execSQL("CREATE TABLE IF NOT EXISTS customer_dues (" +
                "customer_id INTEGER PRIMARY KEY," +
                "due_amount REAL DEFAULT 0," +
                "FOREIGN KEY(customer_id) REFERENCES customers(id)" +
                ")");
        db.execSQL("CREATE TABLE IF NOT EXISTS customer_due_payments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "customer_id INTEGER," +
                "previous_due REAL," +
                "payment_amount REAL," +
                "new_due REAL," +
                "payment_date TEXT," +
                "FOREIGN KEY(customer_id) REFERENCES customers(id)" +
                ")");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE products ADD COLUMN name_marathi TEXT");
        }

        if (oldVersion < 3) {
            db.execSQL("CREATE TABLE IF NOT EXISTS bill_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "bill_number TEXT UNIQUE," +
                    "item_name TEXT," +
                    "quantity REAL," +
                    "rate REAL," +
                    "total REAL," +
                    "FOREIGN KEY(bill_number) REFERENCES bills(bill_number))");
        }

        db.execSQL("DROP TABLE IF EXISTS temp_cart");
        db.execSQL("CREATE TABLE IF NOT EXISTS temp_cart (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "item_name TEXT, " +
                "price REAL, " +
                "unit TEXT, " +
                "quantity REAL, " +
                "session_id TEXT DEFAULT 'BILL1')");

        if (oldVersion < 4) {
            db.execSQL("CREATE TABLE IF NOT EXISTS customer_dues (" +
                    "customer_id INTEGER PRIMARY KEY," +
                    "due_amount REAL DEFAULT 0," +
                    "FOREIGN KEY(customer_id) REFERENCES customers(id)" +
                    ")");
        }

        if (oldVersion < 5) {
            db.execSQL("CREATE TABLE IF NOT EXISTS customer_due_payments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "customer_id INTEGER," +
                    "previous_due REAL," +
                    "payment_amount REAL," +
                    "new_due REAL," +
                    "payment_date TEXT," +
                    "FOREIGN KEY(customer_id) REFERENCES customers(id)" +
                    ")");
        }
    }


    public long insertCustomer(String name, String phone) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if customer already exists by phone or name
        long existingId = -1;
        Cursor cursor = db.rawQuery(
                "SELECT id FROM customers WHERE name = ? OR phone = ?",
                new String[]{name, phone}
        );

        if (cursor.moveToFirst()) {
            existingId = cursor.getLong(0);
            Log.d("DBHelper", "Customer exists: " + name + ", id: " + existingId);
        }
        cursor.close();

        if (existingId != -1) {
            db.close();
            return existingId; // Return existing ID - no duplicate insert
        }

        // Insert new customer
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("phone", phone);

        long newId = db.insert("customers", null, values);
        Log.d("DBHelper", "Inserted new customer: " + name + ", id: " + newId);
        db.close();
        return newId;
    }


    public long insertPurchase(long customerId, String items, double totalAmount, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        String billNumber = generateBillNumber();
        values.put("bill_number", billNumber);
        values.put("customer_id", customerId);
        values.put("items", items);
        values.put("total_amount", totalAmount);
        values.put("date", date);
        return db.insert("purchases", null, values);
    }

    public String generateBillNumber() {
        SQLiteDatabase db = this.getReadableDatabase();
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM purchases WHERE bill_number LIKE ?", new String[]{today + "%"});
        int count = 1;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0) + 1;
        }
        cursor.close();
        return today + "-" + String.format("%03d", count);
    }

    public void insertBill(String billNumber, String customerName, String phone, String date, double total) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("bill_number", billNumber);
        values.put("customer_name", customerName);
        values.put("phone", phone);
        values.put("date", date);
        values.put("total", total);

        long result = db.insert("bills", null, values);
        if (result == -1) {
            Log.e("DBHelper", "InsertBill FAILED ❌: " +
                    "billNumber=" + billNumber +
                    ", customerName=" + customerName +
                    ", phone=" + phone +
                    ", date=" + date +
                    ", total=" + total);
        } else {
            Log.d("DBHelper", "InsertBill SUCCESS ✅: ID=" + result);
        }

    }

    public String getLastCustomerBill() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT customer_name, total FROM bills ORDER BY id DESC LIMIT 1", null);
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            double total = cursor.getDouble(1);
            cursor.close();
            return "Last Bill: " + name + " - ₹" + total;
        }
        cursor.close();
        return "No previous bills";
    }

    public double getTodaySalesTotal() {
        double total = 0;
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(total) FROM bills WHERE date LIKE ?", new String[]{today + "%"});
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }


    public ArrayList<String> getRecentBills() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT customer_name, total, date FROM bills ORDER BY id DESC LIMIT 5", null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            double total = cursor.getDouble(1);
            String date = cursor.getString(2);
            list.add(name + " - ₹" + total + " (" + date + ")");
        }
        cursor.close();
        return list;
    }

    public ArrayList<String> getAllCustomerNames() {
        ArrayList<String> names = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT name FROM customers", null);
        while (cursor.moveToNext()) {
            names.add(cursor.getString(0));
        }
        cursor.close();
        return names;
    }

    public String getPhoneByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT phone FROM customers WHERE name = ? LIMIT 1", new String[]{name});
        if (cursor.moveToFirst()) {
            String phone = cursor.getString(0);
            cursor.close();
            return phone;
        }
        cursor.close();
        return "";
    }

    public ArrayList<String> getBillsBetween(String from, String to) {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT customer_name, total, date FROM bills WHERE date BETWEEN ? AND ? ORDER BY date DESC",
                new String[]{from, to});
        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            double total = cursor.getDouble(1);
            String date = cursor.getString(2);
            list.add(name + " - ₹" + total + " (" + date + ")");
        }
        cursor.close();
        return list;
    }

    // ✅ Export as Excel (if minSdkVersion >= 26)
    public File exportBillsToExcel(Context context) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT bill_number, customer_name, phone, date, total FROM bills", null);

            HSSFWorkbook workbook = new HSSFWorkbook();
            HSSFSheet sheet = workbook.createSheet("Bill History");

            HSSFRow header = sheet.createRow(0);
            header.createCell(0).setCellValue("Bill No");
            header.createCell(1).setCellValue("Customer");
            header.createCell(2).setCellValue("Phone");
            header.createCell(3).setCellValue("Date");
            header.createCell(4).setCellValue("Total");

            int rowNum = 1;
            while (cursor.moveToNext()) {
                HSSFRow row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(cursor.getString(0));
                row.createCell(1).setCellValue(cursor.getString(1));
                row.createCell(2).setCellValue(cursor.getString(2));
                row.createCell(3).setCellValue(cursor.getString(3));
                row.createCell(4).setCellValue(cursor.getDouble(4));
            }
            cursor.close();

            File dir = new File(context.getExternalFilesDir(null), "exports");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "BillHistory.xls");

            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
            workbook.close();

            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Export as CSV (best for Android)
    public File exportBillsToCSV(Context context) {
        File exportDir = new File(context.getExternalFilesDir(null), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();

        File file = new File(exportDir, "bill_history.csv");

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("Bill Number,Customer Name,Phone,Date,Total\n");

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT bill_number, customer_name, phone, date, total FROM bills", null);

            while (cursor.moveToNext()) {
                writer.append(cursor.getString(0)).append(",")
                        .append(cursor.getString(1)).append(",")
                        .append(cursor.getString(2)).append(",")
                        .append(cursor.getString(3)).append(",")
                        .append(String.valueOf(cursor.getDouble(4))).append("\n");
            }

            cursor.close();
            writer.flush();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ✅ Insert Product
    public void insertProduct(String name, String nameMarathi, double price, String category, String unit) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("name_marathi", nameMarathi);
        values.put("price", price);
        values.put("category", category);
        values.put("unit", unit);
        db.insert("products", null, values);
    }


    // ✅ Get All Products
    public Cursor getAllProducts() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT id, name, price, category FROM products ORDER BY name ASC",null);
    }

    // ✅ Get Product by ID
    public Cursor getProductById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM products WHERE id = ?", new String[]{String.valueOf(id)});
    }

    public void updateProduct(int id, String name, String name_marathi, double price, String category, String unit) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("name_marathi", name_marathi);
        values.put("price", price);
        values.put("category", category);
        values.put("unit", unit);
        db.update("products", values, "id = ?", new String[]{String.valueOf(id)});
    }
    // ✅  Price
    public void updateProductPrice(int id, double newPrice) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("price", newPrice);
        db.update("products", values, "id = ?", new String[]{String.valueOf(id)});
    }

    // ✅ Delete Product
    public void deleteProduct(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("products", "id = ?", new String[]{String.valueOf(id)});
    }

    public ArrayList<String> getAllProductNames() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM products", null);
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();
        return list;
    }

    public ArrayList<String> getProductsByCategory(String category) {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM products WHERE category = ?", new String[]{category});
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();
        return list;
    }

    public double getProductPrice(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT price FROM products WHERE name = ? LIMIT 1", new String[]{productName});
        if (cursor.moveToFirst()) {
            double price = cursor.getDouble(0);
            cursor.close();
            return price;
        }
        cursor.close();
        return 0;
    }

    public String getProductUnit(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT unit FROM products WHERE name = ? LIMIT 1", new String[]{productName});
        if (cursor.moveToFirst()) {
            String unit = cursor.getString(0);
            cursor.close();
            return unit;
        }
        cursor.close();
        return "";
    }

    public ArrayList<ProductInfo> getAllProductInfo() {
        ArrayList<ProductInfo> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name, price, unit FROM products", null);
        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            double price = cursor.getDouble(1);
            String unit = cursor.getString(2);
            list.add(new ProductInfo(name, price, unit));
        }
        cursor.close();
        return list;
    }

    public String getProductMarathiName(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name_marathi FROM products WHERE name = ?", new String[]{productName});
        if (cursor.moveToFirst()) {
            String name = cursor.getString(0);
            cursor.close();
            return name;
        }
        cursor.close();
        return productName; // fallback to English
    }

    public void insertDefaultCategories() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("INSERT OR IGNORE INTO categories (name) VALUES ('General'), ('Groceries'), ('Bakery'), ('Dairy')");
    }


    public void insertCategoryIfNotExists(String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM products WHERE category = ?", new String[]{category});
        if (!cursor.moveToFirst()) {
            ContentValues values = new ContentValues();
            values.put("name", "dummy");
            values.put("price", 0);
            values.put("category", category);
            db.insert("products", null, values);
            db.delete("products", "name=? AND price=?", new String[]{"dummy", "0"});
        }
        cursor.close();
    }


    // Get all category names
    // Insert new category
    public boolean insertCategory(String categoryName) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", categoryName);
        long result = db.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        return result != -1;
    }

    public void deleteCategory(String categoryName) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("categories", "name = ?", new String[]{categoryName});
    }

    // Get all category names
    public ArrayList<String> getAllCategories() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT name FROM categories", null);
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();
        return list;
    }

    public String getProductCategory(String productName) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT category FROM products WHERE name = ?", new String[]{productName});
        if (cursor.moveToFirst()) {
            String category = cursor.getString(0);
            cursor.close();
            return category;
        }
        cursor.close();
        return "General"; // Default if not found
    }

    public List<DailySales> getLast7DaysSales() {
        List<DailySales> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT date, SUM(total) as total FROM bills GROUP BY date ORDER BY date DESC LIMIT 7";
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            list.add(new DailySales(
                    cursor.getString(0),
                    cursor.getDouble(1)
            ));
        }
        cursor.close();
        return list;
    }

    public Map<String, Double> getTop5Products() {
        Map<String, Double> map = new LinkedHashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT item_name, SUM(total) as total FROM bill_items GROUP BY item_name ORDER BY total DESC LIMIT 5";
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            map.put(cursor.getString(0), cursor.getDouble(1));
        }
        Log.d("SalesSummary", "Top Products: " + map.size());
        cursor.close();
        return map;
    }

    public class DailySales {
        public String date;
        public double total;

        public DailySales(String date, double total) {
            this.date = date;
            this.total = total;
        }
    }

    public ArrayList<RecentBill> getRecentBillObjects() {
        ArrayList<RecentBill> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM bills ORDER BY date DESC LIMIT 10", null);

        if (cursor.moveToFirst()) {
            do {
                String billNo = cursor.getString(cursor.getColumnIndexOrThrow("bill_number"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String customer = cursor.getString(cursor.getColumnIndexOrThrow("customer_name"));
                double total = cursor.getDouble(cursor.getColumnIndexOrThrow("total"));

                list.add(new RecentBill(billNo, date, customer, total));
            } while (cursor.moveToNext());
        }

        cursor.close();
        return list;
    }

    public double getSalesTotalByDate(String date) {
        double total = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(total) FROM bills WHERE date LIKE ?", new String[]{date +"%"});

        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public int getBillCountByDate(String date) {
        int count = 0;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM bills WHERE date LIKE ?", new String[]{date + "%"});
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public void logRecentBillDates() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT date FROM bills ORDER BY id DESC LIMIT 5", null);

        Log.d("BillDates", "Last 5 Dates:");
        while (cursor.moveToNext()) {
            Log.d("BillDates", cursor.getString(0));
        }

        cursor.close();
    }

    public void logLast5BillDates() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT bill_number, date FROM bills ORDER BY id DESC LIMIT 5", null);

        Log.d("BillDates", "Last 5 Bills:");
        if (cursor.moveToFirst()) {
            do {
                String billNo = cursor.getString(0);
                String date = cursor.getString(1);
                Log.d("BillDates", "Bill No: " + billNo + ", Date: " + date);
            } while (cursor.moveToNext());
        } else {
            Log.d("BillDates", "No bills found.");
        }

        cursor.close();
    }
    public double getAvgSalePerCustomer(String date) {
        double avg = 0;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT AVG(total) FROM bills WHERE date LIKE ?", new String[]{date + "%"});
        if (cursor.moveToFirst()) {
            avg = cursor.getDouble(0);
        }
        cursor.close();
        return avg;
    }

    public File exportSalesSummaryToCSV(Context context, String date) {
        File exportDir = new File(context.getExternalFilesDir(null), "exports");
        if (!exportDir.exists()) exportDir.mkdirs();

        File file = new File(exportDir, "sales_summary_" + date + ".csv");

        try (FileWriter writer = new FileWriter(file)) {
            writer.append("Bill Number,Customer Name,Phone,Date,Total\n");

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT bill_number, customer_name, phone, date, total FROM bills WHERE date LIKE ?",
                    new String[]{date+"%"}
);
            Log.d("ExportCSV", "Exporting for date: " + date);
            Log.d("ExportCSV", "Found rows: " + cursor.getCount());

            while (cursor.moveToNext()) {
                writer.append(cursor.getString(0)).append(",")
                        .append(cursor.getString(1)).append(",")
                        .append(cursor.getString(2)).append(",")
                        .append(cursor.getString(3)).append(",")
                        .append(String.valueOf(cursor.getDouble(4))).append("\n");
            }

            cursor.close();
            writer.flush();
            return file;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
    }
    }
    public double getProductRate(String productName) {
        double rate = 0.0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT price FROM products WHERE name = ?", new String[]{productName});
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                rate = cursor.getDouble(0);  // Assumes 'rate' column is original price (MRP)
            }
            cursor.close();
        }
        return rate;
    }

    public double getPreviousDueAmount(String customerName) {
        SQLiteDatabase db = this.getReadableDatabase();
        double due = 0;
        Cursor cursor = db.rawQuery(
                "SELECT due_amount FROM customer_dues cd " +
                        "JOIN customers c ON cd.customer_id = c.id WHERE c.name = ?",
                new String[]{customerName});

        if (cursor.moveToFirst()) {
            due = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        return due;
    }

    // Update due amount for a customer by ID
    public void updateCustomerDue(long customerId, double newDue) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("due_amount", newDue);

        int rows = db.update("customer_dues", values, "customer_id = ?", new String[]{String.valueOf(customerId)});

        // If no row updated, insert new
        if (rows == 0) {
            values.put("customer_id", customerId);
            db.insert("customer_dues", null, values);
        }
        db.close();
    }
    // Get due amount by customer ID
    public double getDueAmountByCustomerId(long customerId) {
        SQLiteDatabase db = this.getReadableDatabase();
        double due = 0.0;
        Cursor cursor = db.rawQuery("SELECT due_amount FROM customer_dues WHERE customer_id = ?", new String[]{String.valueOf(customerId)});
        if (cursor.moveToFirst()) {
            due = cursor.getDouble(0);
        }
        cursor.close();
        db.close();
        Log.d("DBHelper", "Due for customer_id " + customerId + " is " + due);
        return due;
    }

    public void upsertCustomerDue(long customerId, double dueAmount) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Check if customer due exists
        Cursor cursor = db.rawQuery("SELECT customer_id FROM customer_dues WHERE customer_id = ?", new String[]{String.valueOf(customerId)});
        ContentValues values = new ContentValues();
        values.put("customer_id", customerId);
        values.put("due_amount", dueAmount);

        if (cursor.moveToFirst()) {
            Log.d("DBHelper", "Updating due for customer_id: " + customerId + " due: " + dueAmount);
            db.update("customer_dues", values, "customer_id = ?", new String[]{String.valueOf(customerId)});
        } else {
            Log.d("DBHelper", "Inserting due for customer_id: " + customerId + " due: " + dueAmount);
            db.insert("customer_dues", null, values);
        }

        cursor.close();
        db.close();
    }


    // Get customer id by name
    public long getCustomerIdByName(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        long id = -1;
        Log.i("DBHelper", "getCustomerIdByName for '" + name + "' returns " + id);
        Cursor cursor = db.rawQuery("SELECT id FROM customers WHERE name = ?", new String[]{name});
        if (cursor.moveToFirst()) {
            id = cursor.getLong(0);
        }
        cursor.close();
        db.close();
        Log.d("DBHelper", "getCustomerIdByName for '" + name + "' returns " + id);
        Log.i("DBHelper", "getCustomerIdByName for '" + name + "' returns " + id);
        return id;
    }
    public String getLatestDuePaymentDate(long customerId) {
        String date = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT payment_date FROM customer_due_payments WHERE customer_id = ? ORDER BY payment_date DESC LIMIT 1",
                new String[]{String.valueOf(customerId)}
        );
        if (cursor.moveToFirst()) {
            date = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return date;
    }

    public void insertCustomerDuePayment(long customerId, double previousDue, double paymentAmount, double newDue, String paymentDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("customer_id", customerId);
        values.put("previous_due", previousDue);
        values.put("payment_amount", paymentAmount);
        values.put("new_due", newDue);
        values.put("payment_date", paymentDate);
        db.insert("customer_due_payments", null, values);
        db.close();
    }
    public String getDueDateByCustomerId(long customerId) {
        String dueDate = null;
        SQLiteDatabase db = this.getReadableDatabase();

        // Replace 'due_date' and 'customer_dues' with your actual table and column names
        Cursor cursor = db.rawQuery(
                "SELECT due_date FROM customer_dues WHERE customer_id = ?",
                new String[]{String.valueOf(customerId)}
        );

        if (cursor.moveToFirst()) {
            dueDate = cursor.getString(0);
        }
        cursor.close();
        db.close();

        return dueDate;
    }
    public String getOriginalDueDate(long customerId) {
        String date = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT payment_date FROM customer_due_payments WHERE customer_id = ? ORDER BY payment_date ASC LIMIT 1",
                new String[]{String.valueOf(customerId)}
        );
        if (cursor.moveToFirst()) {
            date = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return date;
    }
    public String getCustomerPhoneById(long customerId) {
        String phone = "";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT phone FROM customers WHERE id = ?",
                new String[]{String.valueOf(customerId)}
        );
        if (cursor.moveToFirst()) {
            phone = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return phone;
    }


}
