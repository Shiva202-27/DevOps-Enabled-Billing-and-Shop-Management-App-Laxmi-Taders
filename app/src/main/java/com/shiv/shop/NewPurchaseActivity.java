package com.shiv.shop;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NewPurchaseActivity extends AppCompatActivity {

    TextView textBillNumber, textCartSummary;
    AutoCompleteTextView editName, autoProductSearch;
    EditText editPhone, editQty, editRate;
    Button btnAddToCart, btnSaveBill;
    RecyclerView listCart;
    TextView textPreviousDue;


    ArrayList<CartItem> cartItems = new ArrayList<>();
    CartAdapter cartAdapter;
    double grandTotal = 0;

    private String currentBillNumber;  // Store current bill ID
    private DBHelper dbHelper;          // DBHelper as a field for all methods

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_purchase);

        // Initialize DBHelper once
        dbHelper = new DBHelper(this);

        // Bind UI views
        textBillNumber = findViewById(R.id.textBillNumber);
        editName = findViewById(R.id.editCustomerName);
        editPhone = findViewById(R.id.editCustomerPhone);
        editQty = findViewById(R.id.editQty);
        editRate = findViewById(R.id.editRate);
        autoProductSearch = findViewById(R.id.autoProductSearch);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnSaveBill = findViewById(R.id.btnSavePurchase);
        textCartSummary = findViewById(R.id.textCartSummary);
        listCart = findViewById(R.id.listCart);
        textPreviousDue = findViewById(R.id.textPreviousDue);

        Spinner spinnerPaymentStatus = findViewById(R.id.spinnerPaymentStatus);
        Button btnBill1 = findViewById(R.id.btnBill1);
        Button btnBill2 = findViewById(R.id.btnBill2);
        Button btnBill3 = findViewById(R.id.btnBill3);
        EditText editPayment = findViewById(R.id.editPayment);
        editName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String customerName = s.toString().trim();
                if (!customerName.isEmpty()) {
                    double previousDue = dbHelper.getPreviousDueAmount(customerName);
                    textPreviousDue.setText(String.format("Due: ₹%.2f", previousDue));
                } else {
                    textPreviousDue.setText("Due: ₹0.00");
                }
            }
        });


        // Initialize cart adapter and recycler view
        cartAdapter = new CartAdapter(this, cartItems);
        listCart.setLayoutManager(new LinearLayoutManager(this));
        listCart.setAdapter(cartAdapter);
        cartAdapter.setCartUpdateListener(this::updateCartSummary);
        String[] paymentStatusArray = {"-- Select Payment Status --", "Paid", "Partial", "Unpaid"};
        ArrayAdapter<String> paymentAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, paymentStatusArray);
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPaymentStatus.setAdapter(paymentAdapter);


        // Set default quantity
        editQty.setText("1");

        // Setup product autocomplete with product info
        ArrayList<ProductInfo> productInfos = dbHelper.getAllProductInfo();
        ArrayAdapter<ProductInfo> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, productInfos);
        autoProductSearch.setAdapter(adapter);
        autoProductSearch.setThreshold(1);

        // Show unit and rate on product selection
        TextView textUnit = findViewById(R.id.textUnit);
        autoProductSearch.setOnItemClickListener((parent, view, position, id) -> {
            ProductInfo selected = (ProductInfo) parent.getItemAtPosition(position);
            autoProductSearch.setText(selected.getName());
            editRate.setText(String.valueOf((int) selected.getPrice()));
            textUnit.setText(selected.getUnit());
        });


        // Setup customer autocomplete
        editName.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, dbHelper.getAllCustomerNames()));
        editName.setThreshold(1);
        editName.setOnItemClickListener((parent, view, position, id) -> {
            String customerName = (String) parent.getItemAtPosition(position);
            editPhone.setText(dbHelper.getPhoneByName(customerName));
        });


        // Add to cart button logic
        btnAddToCart.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(NewPurchaseActivity.this, R.anim.button_click);
            v.startAnimation(anim);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    String product = autoProductSearch.getText().toString().trim();
                    String qtyStr = editQty.getText().toString().trim();
                    String rateStr = editRate.getText().toString().trim();

                    if (product.isEmpty() || qtyStr.isEmpty() || rateStr.isEmpty()) {
                        Toast.makeText(NewPurchaseActivity.this, "Fill product, qty and rate", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double qty = Double.parseDouble(qtyStr);
                    double rate = Double.parseDouble(rateStr);

                    // Fetch additional info from DBHelper
                    String unit = dbHelper.getProductUnit(product);
                    String marathiName = dbHelper.getProductMarathiName(product);
                    String category = dbHelper.getProductCategory(product);
                    double originalPrice = dbHelper.getProductRate(product);


                    // Add to cart list & adapter
                    CartItem item = new CartItem(product, marathiName, rate, qty, unit, category, originalPrice);
                    cartItems.add(item);
                    cartAdapter.notifyDataSetChanged();
                    updateCartSummary();

                    // Insert to temp_cart DB with current session ID
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    ContentValues values = new ContentValues();
                    values.put("item_name", product);
                    values.put("price", rate);
                    values.put("unit", unit);
                    values.put("quantity", qty);
                    values.put("session_id", currentBillNumber); // use currentBillNumber as session_id
                    db.insert("temp_cart", null, values);
                    db.close();

                    // Animate cart summary for feedback
                    textCartSummary.animate()
                            .scaleX(1.2f)
                            .scaleY(1.2f)
                            .setDuration(150)
                            .withEndAction(() -> textCartSummary.animate().scaleX(1f).scaleY(1f).setDuration(150))
                            .start();

                    // Reset input fields
                    autoProductSearch.setText("");
                    editQty.setText("1");
                    editRate.setText("");
                    textUnit.setText("Unit");
                }
            });
        });




        btnSaveBill.setOnClickListener(v -> {
            String paymentStatus = spinnerPaymentStatus.getSelectedItem().toString();
            String paymentReceivedStr = editPayment.getText().toString().trim();

            // Validate payment status
            if (paymentStatus.equals("-- Select Payment Status --")) {
                Toast.makeText(this, "Please select payment status before saving the bill", Toast.LENGTH_SHORT).show();
                return;
            }
            if (paymentStatus.equals("Partial") && paymentReceivedStr.isEmpty()) {
                Toast.makeText(this, "Please enter payment amount for partial payment", Toast.LENGTH_SHORT).show();
                return;
            }

            // Use final array to hold payment for inner class access
            final double[] paymentReceived = new double[1];
            paymentReceived[0] = 0;

            if (!paymentReceivedStr.isEmpty()) {
                try {
                    paymentReceived[0] = Double.parseDouble(paymentReceivedStr);
                    if (paymentReceived[0] < 0) {
                        Toast.makeText(this, "Payment cannot be negative", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid payment amount", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Animation anim = AnimationUtils.loadAnimation(NewPurchaseActivity.this, R.anim.button_click);
            v.startAnimation(anim);

            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationRepeat(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    String name = editName.getText().toString().trim();
                    String phone = editPhone.getText().toString().trim();

                    if (name.isEmpty() || phone.isEmpty() || cartItems.isEmpty()) {
                        Toast.makeText(NewPurchaseActivity.this, "Fill customer details and add items", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String currentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    String billNumber = dbHelper.generateBillNumber();
                    textBillNumber.setText("Bill No: " + billNumber);

                    // Build item string and calculate grand total
                    StringBuilder allItems = new StringBuilder();
                    double grandTotal = 0;
                    for (CartItem item : cartItems) {
                        allItems.append(item.getName()).append(" x ").append(item.getQuantity())
                                .append(" @ ₹").append(item.getRate())
                                .append(" = ₹").append(item.getTotal()).append(", ");
                        grandTotal += item.getTotal();
                    }
                    String itemsString = allItems.toString().replaceAll(", $", "");

                    // Insert customer & purchase
                    long customerId = dbHelper.insertCustomer(name, phone);
                    if (customerId == -1 || dbHelper.insertPurchase(customerId, itemsString, grandTotal, currentDate) == -1) {
                        Toast.makeText(NewPurchaseActivity.this, "❌ Failed to save bill", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Generate PDF and insert bill
                    File billPdf = generateBillPdf(billNumber, name, phone, currentDate);
                    dbHelper.insertBill(billNumber, name, phone, currentDate, grandTotal);


                    // Insert each cart item into DB
                    for (CartItem item : cartItems) {
                        ContentValues values = new ContentValues();
                        values.put("bill_number", billNumber);
                        values.put("item_name", item.getName());
                        values.put("quantity", item.getQuantity());
                        values.put("rate", item.getRate());
                        values.put("total", item.getTotal());
                        dbHelper.getWritableDatabase().insert("bill_items", null, values);
                    }

                    // Update customer's due properly
                    double previousDue = dbHelper.getPreviousDueAmount(name);
                    double unpaidAmount = grandTotal - paymentReceived[0]; // Only unpaid part is added
                    double newDue = previousDue + unpaidAmount;
                    dbHelper.upsertCustomerDue(customerId, newDue);
                    runOnUiThread(() -> textPreviousDue.setText(String.format("₹%.2f", newDue)));

                    // Success popup & animation
                    playSuccessFeedback();
                    View popupView = LayoutInflater.from(NewPurchaseActivity.this)
                            .inflate(R.layout.dialog_success_popup, null);
                    AlertDialog.Builder builder = new AlertDialog.Builder(NewPurchaseActivity.this);
                    builder.setView(popupView);
                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    dialog.show();
                    new Handler().postDelayed(() -> {
                        dialog.dismiss();
                        finish();
                    }, 5000);

                    // Clear cart and inputs
                    editName.setText("");
                    editPhone.setText("");
                    cartItems.clear();
                    cartAdapter.notifyDataSetChanged();
                    updateCartSummary();

                    // Clear temporary cart session
                    SQLiteDatabase db = dbHelper.getWritableDatabase();
                    db.delete("temp_cart", "session_id = ?", new String[]{currentBillNumber});
                    db.close();

                    Toast.makeText(NewPurchaseActivity.this, "✅ Bill Saved! Total ₹" + grandTotal, Toast.LENGTH_LONG).show();
                }
            });
        });


        // Initialize bill to BILL_1 on start
        currentBillNumber = "BILL_1";
        switchBill(currentBillNumber);

        // Bill buttons listeners use switchBill() and save current data first
        btnBill1.setOnClickListener(v -> {
            saveCurrentBillCustomerInfo(currentBillNumber);
            currentBillNumber = "BILL_1";
            switchBill(currentBillNumber);
        });

        btnBill2.setOnClickListener(v -> {
            saveCurrentBillCustomerInfo(currentBillNumber);
            currentBillNumber = "BILL_2";
            switchBill(currentBillNumber);
        });

        btnBill3.setOnClickListener(v -> {
            saveCurrentBillCustomerInfo(currentBillNumber);
            currentBillNumber = "BILL_3";
            switchBill(currentBillNumber);
        });
    }

    private void switchBill(String billId) {
        currentBillNumber = billId;           // Keep currentBillNumber updated
        SessionManager.setCurrentSessionId(billId);
        loadBillCustomerInfo(billId);
        loadCartFromDatabase(billId);
        textBillNumber.setText("Bill No: " + billId);
    }

    private void saveCurrentBillCustomerInfo(String billNumber) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("customer_name", editName.getText().toString());
        values.put("phone", editPhone.getText().toString());

        int rows = db.update("bills", values, "bill_number=?", new String[]{billNumber});
        if (rows == 0) {
            values.put("bill_number", billNumber);
            values.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
            values.put("total", 0.0);
            db.insert("bills", null, values);
        }
        db.close();
    }

    private void loadBillCustomerInfo(String billNumber) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT customer_name, phone FROM bills WHERE bill_number=?",
                new String[]{billNumber}
        );
        Log.d("DebugCheck", "loadBillCustomerInfo called with billNumber = " + billNumber);

        if (cursor.moveToFirst()) {
            String customerName = cursor.getString(0);
            editName.setText(customerName);
            editPhone.setText(cursor.getString(1));
            Log.d("DebugCheck", "Customer name loaded: " + customerName);
            Log.d("DebugCheck", "Calling getCustomerIdByName for: " + customerName);

            long customerId = dbHelper.getCustomerIdByName(customerName);
            Log.d("DebugCheck", "getCustomerIdByName returned: " + customerId);

            double due = 0;
            if (customerId != -1) {
                Log.d("DebugCheck", "Calling getDueAmountByCustomerId for id: " + customerId);
                due = dbHelper.getDueAmountByCustomerId(customerId);
                Log.d("DebugCheck", "getDueAmountByCustomerId returned: " + due);
            }
            textPreviousDue.setText(String.format("₹%.2f", due));
        } else {
            editName.setText("");
            editPhone.setText("");
            textPreviousDue.setText("₹0.00");
        }
        cursor.close();
        db.close();
    }

    private void loadCartFromDatabase(String billId) {
        cartItems.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "temp_cart",
                new String[]{"item_name", "price", "unit", "quantity"},
                "session_id = ?",
                new String[]{billId},
                null,
                null,
                null
        );

        if (cursor.moveToFirst()) {
            do {
                String itemName = cursor.getString(cursor.getColumnIndexOrThrow("item_name"));
                double price = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                String unit = cursor.getString(cursor.getColumnIndexOrThrow("unit"));
                double quantity = cursor.getDouble(cursor.getColumnIndexOrThrow("quantity"));

                String marathiName = dbHelper.getProductMarathiName(itemName);
                String category = dbHelper.getProductCategory(itemName);
                double originalPrice = dbHelper.getProductRate(itemName);

                CartItem item = new CartItem(itemName, marathiName, price, quantity, unit, category, originalPrice);
                cartItems.add(item);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        cartAdapter.notifyDataSetChanged();
        updateCartSummary();
    }

    private void updateCartSummary() {
        int totalItems = 0;
        double totalPrice = 0;
        for (CartItem item : cartItems) {
            totalItems += item.getQuantity();
            totalPrice += item.getTotal();
        }
        textCartSummary.setText("🧺 Cart: (" + totalItems + " items) Total: ₹" + totalPrice);
    }


    private File generateBillPdf(String billNumber, String customerName, String phone, String dateTime) {
        try {
            String pdfName = "Bill_" + billNumber + ".pdf";
            File dir = new File(getExternalFilesDir(null), "bills");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, pdfName);

            PdfDocument document = new PdfDocument();
            int pageWidth = 227;
            int baseHeight = 300;
            int itemHeight = 18;
            int qrCodeHeight = 180;
            int maxHeight = 1200;

            int totalHeight = baseHeight + (cartItems.size() * itemHeight) + qrCodeHeight;
            totalHeight = Math.min(totalHeight, maxHeight);

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, totalHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();

            int y = 20;
            paint.setTypeface(Typeface.MONOSPACE);
            paint.setTextSize(12);
            paint.setFakeBoldText(true);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("LAXMI TRADERS", pageWidth / 2, y, paint);
            y += 16;

            paint.setTextSize(9);
            paint.setFakeBoldText(false);
            canvas.drawText("Mob: 7769013190, 8793418382", pageWidth / 2, y, paint);
            y += 14;

            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Date: " + dateTime, 10, y, paint);
            y += 14;
            canvas.drawText("Bill No: " + billNumber, 10, y, paint);
            y += 16;

            String customerLine = "Customer: " + customerName + " (" + phone + ")";
            float customerTextWidth = paint.measureText(customerLine);
            canvas.drawText(customerLine, pageWidth - customerTextWidth - 10, y, paint);
            y += 10;

            canvas.drawLine(0, y, pageWidth, y, paint);
            y += 12;

            int xItem = 10, xQty = 92, xRate = 140, xTotal = 190;
            paint.setFakeBoldText(true);
            canvas.drawText("Item(वस्तू)", xItem, y, paint);
            canvas.drawText("Qty(प्र)", xQty, y, paint);
            canvas.drawText("Rate(दर)", xRate, y, paint);
            canvas.drawText("Total(रु)", xTotal, y, paint);
            y += 6;
            canvas.drawLine(0, y, pageWidth, y, paint);
            y += 12;

            paint.setFakeBoldText(false);
            paint.setTypeface(Typeface.MONOSPACE);

            double totalAmount = 0;
            double totalDiscount = 0;
            int itemNumber = 1;
            int boxTop = y - 10;

            // Loop through cart items only
            for (CartItem item : cartItems) {
                String itemName = "[" + itemNumber + "] " + item.getMarathiName();
                String qtyWithUnit = item.getQuantity() == (int) item.getQuantity()
                        ? ((int) item.getQuantity()) + " " + item.getUnit()
                        : item.getQuantity() + " " + item.getUnit();

                double rate = item.getRate();
                double total = item.getTotal();
                double originalPrice = item.getOriginalPrice();

                paint.setTextSize(9);
                canvas.drawText(itemName, xItem, y, paint);
                canvas.drawText(qtyWithUnit, xQty, y, paint);

                if (rate < originalPrice) {
                    canvas.drawText("₹" + (int) rate + " (₹" + (int) originalPrice + ")", xRate, y, paint);
                    totalDiscount += (originalPrice - rate) * item.getQuantity();
                } else {
                    canvas.drawText("₹" + (int) rate, xRate, y, paint);
                }

                canvas.drawText("₹" + (int) total, xTotal, y, paint);
                y += 16;
                totalAmount += total;
                itemNumber++;
            }

            // Draw table border
            int boxBottom = y - 4;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(0.5f);
            canvas.drawRect(5, boxTop, pageWidth - 5, boxBottom, paint);
            paint.setStyle(Paint.Style.FILL);

            // Summary section
            paint.setFakeBoldText(true);
            canvas.drawLine(0, y, pageWidth, y, paint);
            y += 14;

            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Items: " + cartItems.size(), 10, y, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("Grand Total(रक्कम): ₹" + (int) totalAmount, pageWidth - 10, y, paint);
            y += 14;

            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Discount: ₹" + (int) totalDiscount, 10, y, paint);
            y += 30;

            // QR code
            try {
                String upiId = "shivk202@ybl";
                String payeeName = "Laxmi Traders";
                String upiUri = "upi://pay?pa=" + upiId + "&pn=" + Uri.encode(payeeName) + "&am=" + (int) totalAmount + "&cu=INR";

                QRCodeWriter writer = new QRCodeWriter();
                BitMatrix bitMatrix = writer.encode(upiUri, BarcodeFormat.QR_CODE, 150, 150);
                Bitmap qrBitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888);
                for (int x = 0; x < 150; x++) {
                    for (int z = 0; z < 150; z++) {
                        qrBitmap.setPixel(x, z, bitMatrix.get(x, z) ? Color.BLACK : Color.WHITE);
                    }
                }

                canvas.drawBitmap(qrBitmap, (pageWidth - 150) / 2f, y + 20, null);
                y += 30;
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("Scan to Pay", pageWidth / 2, y + 10, paint);
                canvas.drawText("Thank you for shopping!", pageWidth / 2f, y, paint);

            } catch (Exception e) {
                e.printStackTrace();
            }

            document.finishPage(page);
            document.writeTo(new FileOutputStream(file));
            document.close();
            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void openPdf(File pdfFile) {
        try {
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".provider",
                    pdfFile);


        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
        }
    }

    private void playSuccessFeedback() {
        // Vibration
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(150); // Deprecated in API 26+, but still works
            }
        }

        // Success Sound
        MediaPlayer player = MediaPlayer.create(NewPurchaseActivity.this, Settings.System.DEFAULT_NOTIFICATION_URI);
        if (player != null) {
            player.setOnCompletionListener(MediaPlayer::release);
            player.start();
        }
    }
    private boolean isNumberInContacts(String phoneNumber) {
        // Remove all non-digit characters
        String cleanedNumber = phoneNumber.replaceAll("[^0-9]", "");

        // Ensure it starts with country code (India = 91)
        if (!cleanedNumber.startsWith("91")) {
            cleanedNumber = "91" + cleanedNumber;
        }

        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(cleanedNumber)
        );

        String[] projection = {ContactsContract.PhoneLookup._ID};
        Cursor cursor = null;
        boolean isInContacts = false;
        try {
            cursor = getContentResolver().query(lookupUri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                isInContacts = true; // Number exists in contacts
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) cursor.close();
        }
        return isInContacts;
    }



}