package com.shiv.shop;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.shiv.shop.Product;

import java.util.ArrayList;

public class ProductListAdapter extends BaseAdapter implements Filterable {
    private Context context;
    private ArrayList<Product> productList;
    private ArrayList<Product> filteredList;

    public ProductListAdapter(Context context, ArrayList<Product> productList) {
        this.context = context;
        this.productList = productList;
        this.filteredList = new ArrayList<>(productList);
    }

    @Override
    public int getCount() {
        return filteredList.size();
    }

    @Override
    public Object getItem(int position) {
        return filteredList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return filteredList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Product product = filteredList.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.product_item_view, parent, false);
        }

        TextView name = convertView.findViewById(R.id.textProductName);
        TextView price = convertView.findViewById(R.id.textPrice);
        TextView category = convertView.findViewById(R.id.textCategory);

        name.setText(product.getName());
        price.setText("₹" + product.getPrice());
        category.setText(product.getCategory());

        return convertView;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                ArrayList<Product> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(productList);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Product p : productList) {
                        if (p.getName().toLowerCase().contains(filterPattern) ||
                                p.getCategory().toLowerCase().contains(filterPattern)) {
                            filtered.add(p);
                        }
                    }
                }
                FilterResults results = new FilterResults();
                results.values = filtered;
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredList.clear();
                filteredList.addAll((ArrayList<Product>) results.values);
                notifyDataSetChanged();
            }
  };
}
}