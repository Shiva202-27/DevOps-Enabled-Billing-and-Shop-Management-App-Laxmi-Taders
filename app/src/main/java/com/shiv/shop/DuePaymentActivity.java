package com.shiv.shop;
import static androidx.core.content.ContextCompat.startActivity;

import androidx.core.content.FileProvider;
import android.net.Uri;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DuePaymentActivity extends AppCompatActivity {

    private RecyclerView recyclerCustomerDues;
    private CustomerDueAdapter customerDueAdapter;

    private AutoCompleteTextView editCustomerName;
    private EditText editPaymentAmount;
    private Button btnSubmitPayment;
    private TextView textUpdatedDue;

    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_due_payment);

        dbHelper = new DBHelper(this);

        editCustomerName = findViewById(R.id.editCustomerName);
        editPaymentAmount = findViewById(R.id.editPaymentAmount);
        btnSubmitPayment = findViewById(R.id.btnSubmitPayment);
        textUpdatedDue = findViewById(R.id.textUpdatedDue);

        // Setup autocomplete for customer names
        ArrayAdapter<String> customerNameAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                dbHelper.getAllCustomerNames()
        );
        editCustomerName.setAdapter(customerNameAdapter);
        editCustomerName.setThreshold(1);

        // When a name is selected from the dropdown
        editCustomerName.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCustomer = (String) parent.getItemAtPosition(position);
            showCustomerDue(selectedCustomer);
        });

        // When the user types a name manually
        editCustomerName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String typedName = s.toString().trim();
                if (!typedName.isEmpty()) {
                    showCustomerDue(typedName);
                } else {
                    textUpdatedDue.setText("Current Due: ₹0.00");
                }
            }
        });

        btnSubmitPayment.setOnClickListener(v -> {
            String customerName = editCustomerName.getText().toString().trim();
            String paymentStr = editPaymentAmount.getText().toString().trim();

            if (TextUtils.isEmpty(customerName)) {
                Toast.makeText(this, "Please enter customer name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(paymentStr)) {
                Toast.makeText(this, "Please enter payment amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double paymentAmount;
            try {
                paymentAmount = Double.parseDouble(paymentStr);
                if (paymentAmount <= 0) {
                    Toast.makeText(this, "Payment amount must be greater than zero", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid payment amount", Toast.LENGTH_SHORT).show();
                return;
            }

            processPayment(customerName, paymentAmount);
            showTotalDue();
        });

        recyclerCustomerDues = findViewById(R.id.recyclerCustomerDues);
        recyclerCustomerDues.setLayoutManager(new LinearLayoutManager(this));
        customerDueAdapter = new CustomerDueAdapter(new ArrayList<>());
        recyclerCustomerDues.setAdapter(customerDueAdapter);

        loadAllCustomerDues();
    }

    private File generateDuePaymentReceiptPdf(String receiptNo, String customerName, String customerPhone,
                                              double previousDue, double amountPaid, double newDue,
                                              String paymentDateTime,String originalDueDate) {
        try {
            String pdfName = "DueReceipt_" + receiptNo + ".pdf";
            File dir = new File(getExternalFilesDir(null), "due_receipts");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, pdfName);

            PdfDocument document = new PdfDocument();

            int pageWidth = 227; // ~80mm printer width in points
            int pageHeight = 400;

            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();

            int y = 30;
            int lineHeight = 20;

            // Header - Shop Name centered, bold, large
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            paint.setTextSize(14);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Shop Name", pageWidth / 2f, y, paint);

            y += lineHeight;
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
            paint.setTextSize(10);
            canvas.drawText("Mob: 1234567890", pageWidth / 2f, y, paint);

            // Draw horizontal line
            y += lineHeight;
            paint.setStrokeWidth(1);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawLine(5, y, pageWidth - 5, y, paint);

            // Reset style for body text
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(11);
            paint.setTextAlign(Paint.Align.LEFT);

            y += lineHeight;

            // Receipt info
            canvas.drawText("Date: " + paymentDateTime, 10, y, paint);
            y += lineHeight;
            canvas.drawText("Receipt No: " + receiptNo, 10, y, paint);
            y += lineHeight;

            // Customer info
            canvas.drawText("Customer: " + customerName + " (" + customerPhone + ")", 10, y, paint);
            y += lineHeight;

            canvas.drawText("Original Due Date: " + (originalDueDate != null ? originalDueDate : "N/A"), 10, y, paint);            y += lineHeight;
            // Due details
            canvas.drawText(String.format("Previous Due: ₹%.2f", previousDue), 10, y, paint);
            y += lineHeight;
            canvas.drawText(String.format("Amount Paid: ₹%.2f", amountPaid), 10, y, paint);
            y += lineHeight;
            canvas.drawText(String.format("New Due: ₹%.2f", newDue), 10, y, paint);
            y += lineHeight;

            // Payment date/time (can be same as receipt date or different)
            canvas.drawText("Payment Date/Time:", 10, y, paint);
            y += 16;
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC));
            paint.setTextSize(10);
            canvas.drawText(paymentDateTime, 10, y, paint);

            y += 40;

            // Footer - thank you message centered
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
            paint.setTextSize(12);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Thank you for your payment!", pageWidth / 2f, y, paint);

            document.finishPage(page);

            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            return file;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    private void loadAllCustomerDues() {
        List<CustomerDue> list = new ArrayList<>();
        List<String> customerNames = dbHelper.getAllCustomerNames();

        for (String name : customerNames) {
            long id = dbHelper.getCustomerIdByName(name);
            if (id != -1) {
                double due = dbHelper.getDueAmountByCustomerId(id);
                list.add(new CustomerDue(id, name, due));
            }
        }
        customerDueAdapter.updateData(list);
    }

    private void showCustomerDue(String customerName) {
        long customerId = dbHelper.getCustomerIdByName(customerName);
        if (customerId == -1) {
            textUpdatedDue.setText("Customer not found");
            return;
        }
        double due = dbHelper.getDueAmountByCustomerId(customerId);
        textUpdatedDue.setText(String.format("Current Due: ₹%.2f", due));
    }

    private void processPayment(String customerName, double paymentAmount) {
        long customerId = dbHelper.getCustomerIdByName(customerName);
        if (customerId == -1) {
            Toast.makeText(this, "Customer not found", Toast.LENGTH_SHORT).show();
            return;


        }

        double previousDue = dbHelper.getDueAmountByCustomerId(customerId);
        double newDue = previousDue - paymentAmount;
        if (newDue < 0) newDue = 0;

        dbHelper.upsertCustomerDue(customerId, newDue);
        textUpdatedDue.setText(String.format("Current Due: ₹%.2f", newDue));

        // Log payment event with date/time
        String paymentDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        dbHelper.insertCustomerDuePayment(customerId, previousDue, paymentAmount, newDue, paymentDate);

        Toast.makeText(this, "Payment of ₹" + paymentAmount + " recorded", Toast.LENGTH_LONG).show();
        editPaymentAmount.setText("");

        // Refresh customer dues list after payment
        loadAllCustomerDues();
        showTotalDue();
        // Generate receipt number (simple example)
        String receiptNo = String.valueOf(System.currentTimeMillis());

        // Get current date/time string
        String paymentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String originalDueDate = dbHelper.getOriginalDueDate(customerId);

        // Generate and save PDF receipt
        String customerPhone = dbHelper.getCustomerPhoneById(customerId);
        File pdfFile = generateDuePaymentReceiptPdf(receiptNo, customerName, customerPhone, previousDue, paymentAmount, newDue, paymentDateTime, originalDueDate);

        if (pdfFile != null) {
            Toast.makeText(this, "Receipt saved: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            // Optionally open PDF or share it here
        }
        if (pdfFile != null) {
            Uri pdfUri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    pdfFile
            );

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);

            startActivity(intent);
        }
    }

            private void showTotalDue () {
                List<String> customerNames = dbHelper.getAllCustomerNames();
                double totalDue = 0;
                for (String name : customerNames) {
                    long id = dbHelper.getCustomerIdByName(name);
                    if (id != -1) {
                        totalDue += dbHelper.getDueAmountByCustomerId(id);
                    }
                }
                TextView textTotalDue = findViewById(R.id.textTotalDue);
                textTotalDue.setText(String.format("Total Due: ₹%.2f", totalDue));
            }
        }

