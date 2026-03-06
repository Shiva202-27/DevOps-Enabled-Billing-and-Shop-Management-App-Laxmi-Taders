package com.shiv.shop;

import android.app.AlertDialog; import android.content.Intent; import android.database.Cursor; import android.os.Bundle; import android.view.View; import android.widget.*; import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ManageProductsActivity extends AppCompatActivity {

    EditText editProductName, editProductPrice, editProductNameMarathi;
    Button btnAddProduct, btnManageCategories;
    ListView listProducts;
    Spinner spinnerCategory, spinnerUnit;
    TextView txtAllProducts;
    SearchView searchView;
    DBHelper dbHelper;

    ProductListAdapter adapter;
    ArrayList<Product> productList;
    ArrayList<Integer> productIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_products);
        // Initialize UI elements
        editProductName = findViewById(R.id.editProductName);
        editProductNameMarathi = findViewById(R.id.editProductNameMarathi);
        editProductPrice = findViewById(R.id.editProductPrice);
        btnAddProduct = findViewById(R.id.btnAddProduct);
        btnManageCategories = findViewById(R.id.btnManageCategories);
        listProducts = findViewById(R.id.listProducts);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        searchView = findViewById(R.id.searchProduct);
        txtAllProducts = findViewById(R.id.txtAllProducts);

        // Initialize database helper
        dbHelper = new DBHelper(this);
        dbHelper.insertDefaultCategories();

        // Setup unit spinner
        String[] units = {"kg", "0.5 kg", "dz", "0.5 dz", "ctn", "bag", "gtu", "pc", "pkt", "bx", "bt", "jar","stp"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, units);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUnit.setAdapter(unitAdapter);

        // Load data
        loadCategories();
        loadProducts();

        // Add Product button
        btnAddProduct.setOnClickListener(v -> {
            String name = editProductName.getText().toString().trim();
            String marathiName = editProductNameMarathi.getText().toString().trim();
            String priceStr = editProductPrice.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String unit = spinnerUnit.getSelectedItem().toString();

            if (name.isEmpty() || priceStr.isEmpty()) {
                Toast.makeText(this, "Enter name and price", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            dbHelper.insertProduct(name, marathiName, price, category, unit);
            Toast.makeText(this, "Product added", Toast.LENGTH_SHORT).show();

            editProductName.setText("");
            editProductNameMarathi.setText("");
            editProductPrice.setText("");
            spinnerUnit.setSelection(0);
            loadProducts();
        });

        // Manage categories
        btnManageCategories.setOnClickListener(v -> {
            Intent intent = new Intent(this, ManageCategoriesActivity.class);
            startActivity(intent);
        });

        // Handle item long click for delete
        listProducts.setOnItemLongClickListener((parent, view, position, id) -> {
            int productId = productIds.get(position);
            new AlertDialog.Builder(this)
                    .setTitle("Delete Product")
                    .setMessage("Are you sure you want to delete this product?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.deleteProduct(productId);
                        loadProducts();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
        // Handle item click for editing
        listProducts.setOnItemClickListener((parent, view, position, id) -> {
            int productId = productIds.get(position);

            // Fetch current product info
            Cursor cursor = dbHelper.getProductById(productId);
            if (cursor.moveToFirst()) {
                String currentName = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                String currentMarathiName = cursor.getString(cursor.getColumnIndexOrThrow("name_marathi"));
                double currentPrice = cursor.getDouble(cursor.getColumnIndexOrThrow("price"));
                String currentCategory = cursor.getString(cursor.getColumnIndexOrThrow("category"));
                String currentUnit = cursor.getString(cursor.getColumnIndexOrThrow("unit"));

                View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_item, null);
                EditText editName = dialogView.findViewById(R.id.editProductName);
                EditText editNameMarathi = dialogView.findViewById(R.id.editProductNameMarathi);
                EditText editPrice = dialogView.findViewById(R.id.editProductPrice);
                Spinner spinnerCategoryDialog = dialogView.findViewById(R.id.spinnerCategory);
                Spinner spinnerUnitDialog = dialogView.findViewById(R.id.spinnerUnit);

                editName.setText(currentName);
                editNameMarathi.setText(currentMarathiName);
                editPrice.setText(String.valueOf(currentPrice));

                // Load categories into spinner
                ArrayList<String> categories = dbHelper.getAllCategories();
                ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
                catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCategoryDialog.setAdapter(catAdapter);
                int categoryPos = categories.indexOf(currentCategory);
                if (categoryPos >= 0) spinnerCategoryDialog.setSelection(categoryPos);

                // Load units into spinner
                unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerUnitDialog.setAdapter(unitAdapter);
                int unitPos = java.util.Arrays.asList(units).indexOf(currentUnit);
                if (unitPos >= 0) spinnerUnitDialog.setSelection(unitPos);

                new AlertDialog.Builder(this)
                        .setTitle("Edit Product")
                        .setView(dialogView)
                        .setPositiveButton("Update", (dialog, which) -> {
                            String newName = editName.getText().toString().trim();
                            String newMarathiName = editNameMarathi.getText().toString().trim();
                            double newPrice = Double.parseDouble(editPrice.getText().toString().trim());
                            String newCategory = spinnerCategoryDialog.getSelectedItem().toString();
                            String newUnit = spinnerUnitDialog.getSelectedItem().toString();

                            dbHelper.updateProduct(productId, newName, newMarathiName, newPrice, newCategory, newUnit);
                            loadProducts(); // refresh
                            Toast.makeText(this, "Product updated", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
            cursor.close();
        });



        // Search functionality
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (adapter != null) {
                    ((Filterable) adapter).getFilter().filter(newText);
                }
                return true;
}
        });
    }


    private void loadProducts() {
        productList = new ArrayList<>();
        productIds = new ArrayList<>();

        Cursor cursor = dbHelper.getAllProducts();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(0);
            String name = cursor.getString(1);
            double price = cursor.getDouble(2);
            String category = cursor.getString(cursor.getColumnIndexOrThrow("category"));

            productList.add(new Product(id, name, price, category));
            productIds.add(id);
        }
        cursor.close();

        adapter = new ProductListAdapter(this, productList);
        listProducts.setAdapter(adapter);
        txtAllProducts.setText("All Products (" + productList.size() + ")");
    }
    private void loadCategories() {
        ArrayList<String> categories = dbHelper.getAllCategories();
        if (categories.isEmpty()) {
            categories.add("General"); // fallback
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();  // refresh every time the activity resumes
    }

}