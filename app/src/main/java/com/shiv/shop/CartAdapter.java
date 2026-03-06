package com.shiv.shop;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
    Context context;
    ArrayList<CartItem> cartItems;
    private CartUpdateListener listener;

    public interface CartUpdateListener {
        void onCartUpdated();
    }

    public void setCartUpdateListener(CartUpdateListener listener) {
        this.listener = listener;
    }

    public CartAdapter(Context context, ArrayList<CartItem> items) {
        this.context = context;
        this.cartItems = items;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_item_view, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder h, @SuppressLint("RecyclerView") int position) {
        CartItem item = cartItems.get(position);

        h.textProductName.setText(item.getName());
        h.textQty.setText(String.valueOf(item.getQuantity()));

        // ✅ Remove old watcher if already added
        if (h.currentTextWatcher != null) {
            h.editPrice.removeTextChangedListener(h.currentTextWatcher);
        }

        // ✅ Set current rate
        h.editPrice.setText(String.valueOf(item.getRate()));
        h.textTotalPrice.setText("₹" + item.getTotal());

        // ✅ New TextWatcher
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    double newRate = Double.parseDouble(s.toString());
                    item.setPrice(newRate);
                    h.textTotalPrice.setText("₹" + item.getTotal());
                    if (listener != null) listener.onCartUpdated();
                } catch (Exception ignored) {}
            }
        };

        h.editPrice.addTextChangedListener(watcher);
        h.currentTextWatcher = watcher;

        h.btnPlus.setOnClickListener(v -> {
            item.setQuantity(item.getQuantity() + 1);
            notifyItemChanged(position);
            if (listener != null) listener.onCartUpdated();
        });

        h.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                notifyItemChanged(position);
                if (listener != null) listener.onCartUpdated();
            }
        });

        h.btnRemove.setOnClickListener(v -> {
            cartItems.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, cartItems.size());
            if (listener != null) listener.onCartUpdated();
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView textProductName, textQty, textTotalPrice;
        EditText editPrice;
        ImageButton btnMinus, btnPlus, btnRemove;
        TextWatcher currentTextWatcher;  // ✅ Holds current TextWatcher

        public CartViewHolder(@NonNull View v) {
            super(v);
            textProductName = v.findViewById(R.id.textProductName);
            editPrice = v.findViewById(R.id.editRate);
            textQty = v.findViewById(R.id.textQty);
            textTotalPrice = v.findViewById(R.id.textTotal);
            btnMinus = v.findViewById(R.id.btnMinus);
            btnPlus = v.findViewById(R.id.btnPlus);
            btnRemove = v.findViewById(R.id.btnRemove);
        }
    }
}