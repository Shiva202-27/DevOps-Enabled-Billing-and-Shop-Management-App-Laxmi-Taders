package com.shiv.shop;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CartActivity extends AppCompatActivity {

    RecyclerView listCart;
    ArrayList<CartItem> cartItems;
    CartAdapter cartAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        listCart = findViewById(R.id.listCart);
        listCart.setLayoutManager(new LinearLayoutManager(this)); // Important for RecyclerView!

        cartItems = new ArrayList<>();
        // TODO: Add sample items to cartItems

        cartAdapter = new CartAdapter(this, cartItems);
        listCart.setAdapter(cartAdapter); // ✅ Works now
    }
}