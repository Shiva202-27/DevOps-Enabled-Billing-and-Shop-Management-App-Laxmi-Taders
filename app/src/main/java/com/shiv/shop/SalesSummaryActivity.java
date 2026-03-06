package com.shiv.shop;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SalesSummaryActivity extends AppCompatActivity {

    BarChart salesBarChart;
    PieChart topProductsChart;
    DBHelper dbHelper;
    TextView textDateSales; // ✅ Declare globally

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_summary);

        dbHelper = new DBHelper(this);

        salesBarChart = findViewById(R.id.salesBarChart);
        topProductsChart = findViewById(R.id.topProductsChart);
        textDateSales = findViewById(R.id.textDateSales); // ✅ Linked once here

        dbHelper.logRecentBillDates();

        showDailySalesChart();
        showTopProductsChart();

        Button btnSelectDate = findViewById(R.id.btnSelectDate);
        Button btnExportSales = findViewById(R.id.btnExportSales);

        btnSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(SalesSummaryActivity.this,
                    (DatePicker view, int y, int m, int d) -> {
                        String selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d);
                        Log.d("SalesSummary", "Selected Date: " + selectedDate);
                        showSalesForDate(selectedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });


        btnExportSales.setOnClickListener(v -> {
            Log.d("SalesSummary", "Export button clicked ✅");

            // Date picker to select date
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(SalesSummaryActivity.this,
                    (view, y, m, d) -> {
                        String selectedDate = String.format("%04d-%02d-%02d", y, m + 1, d);
                        Log.d("SalesSummary", "Selected for export: " + selectedDate);

                        File file = dbHelper.exportSalesSummaryToCSV(this, selectedDate); // ← See method below
                        if (file != null) {
                            Toast.makeText(this, "Exported to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Export failed!", Toast.LENGTH_SHORT).show();
                        }
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        Log.d("SalesSummary", "Calling showSalesForDate with: " + today);
        showSalesForDate(today); // ✅ Automatically show today’s sales
    }

    private void showSalesForDate(String date) {
        Log.d("SalesSummary", "Fetching sales for: " + date);

        double totalSales = dbHelper.getSalesTotalByDate(date);
        int billCount = dbHelper.getBillCountByDate(date);

        Log.d("SalesSummary", "Bills: " + billCount + ", Total Sales: ₹" + totalSales);

        String summary = "📅 " + date +
                "\n🧾 Bills: " + billCount +
                "\n💰 Total Sales: ₹" + totalSales;

        textDateSales.setText(summary);
    }

    private void showDailySalesChart() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();

        List<DBHelper.DailySales> dailySalesList = dbHelper.getLast7DaysSales();
        int i = 0;
        for (DBHelper.DailySales sale : dailySalesList) {
            entries.add(new BarEntry(i, (float) sale.total));
            labels.add(sale.date);
            i++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Daily Sales");
        BarData data = new BarData(dataSet);
        salesBarChart.setData(data);
        salesBarChart.invalidate();
    }

    private void showTopProductsChart() {
        TextView textViewPlaceholder = findViewById(R.id.textViewPlaceholder);
        ArrayList<PieEntry> entries = new ArrayList<>();
        Map<String, Double> topProducts = dbHelper.getTop5Products();

        if (topProducts.isEmpty()) {
            textViewPlaceholder.setVisibility(View.VISIBLE);
            topProductsChart.setVisibility(View.GONE);
            return;
        }

        textViewPlaceholder.setVisibility(View.GONE);
        topProductsChart.setVisibility(View.VISIBLE);

        for (Map.Entry<String, Double> entry : topProducts.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Top Products");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        topProductsChart.setData(pieData);

        topProductsChart.setUsePercentValues(true);
        topProductsChart.getDescription().setEnabled(false);
        topProductsChart.setEntryLabelColor(Color.BLACK);
        topProductsChart.setCenterText("Top 5 Products");
        topProductsChart.setCenterTextSize(18f);
        topProductsChart.setDrawHoleEnabled(true);
        topProductsChart.setHoleRadius(45f);
        topProductsChart.setTransparentCircleRadius(50f);

        topProductsChart.invalidate();
    }

}