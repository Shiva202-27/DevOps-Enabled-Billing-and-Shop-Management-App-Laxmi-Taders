package com.shiv.shop;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class ManageCategoriesActivity extends AppCompatActivity {

    EditText editCategoryName;
    Button btnAddCategory;
    ListView listCategories;
    DBHelper dbHelper;
    ArrayAdapter<String> adapter;
    ArrayList<String> categoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_categories);

        editCategoryName = findViewById(R.id.editCategoryName);
        btnAddCategory = findViewById(R.id.btnAddCategory);
        listCategories = findViewById(R.id.listCategories);

        dbHelper = new DBHelper(this);

        loadCategories();

        btnAddCategory.setOnClickListener(v -> {
            String category = editCategoryName.getText().toString().trim();

            if (category.isEmpty()) {
                Toast.makeText(this, "Enter category name", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean success = dbHelper.insertCategory(category);
            if (success) {
                Toast.makeText(this, "Category added", Toast.LENGTH_SHORT).show();
                editCategoryName.setText("");
                loadCategories();
            } else {
                Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
            }
        });

        listCategories.setOnItemLongClickListener((parent, view, position, id) -> {
            String selectedCategory = categoryList.get(position);
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Category")
                    .setMessage("Delete \"" + selectedCategory + "\"?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        dbHelper.deleteCategory(selectedCategory);
                        loadCategories();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    private void loadCategories() {
        categoryList = dbHelper.getAllCategories();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, categoryList);
        listCategories.setAdapter(adapter);
    }
}
