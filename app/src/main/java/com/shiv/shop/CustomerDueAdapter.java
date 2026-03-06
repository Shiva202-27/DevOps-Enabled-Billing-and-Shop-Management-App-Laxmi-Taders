package com.shiv.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CustomerDueAdapter extends RecyclerView.Adapter<CustomerDueAdapter.ViewHolder> {

    private List<CustomerDue> customerDueList;

    public CustomerDueAdapter(List<CustomerDue> customerDueList) {
        this.customerDueList = customerDueList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_customer_due, parent, false); // your layout filename
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CustomerDue customerDue = customerDueList.get(position);
        holder.textCustomerName.setText(customerDue.getCustomerName());
        holder.textCustomerDue.setText(String.format("₹%.2f", customerDue.getDueAmount()));
    }

    @Override
    public int getItemCount() {
        return customerDueList.size();
    }

    public void updateData(List<CustomerDue> newList) {
        this.customerDueList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textCustomerName, textCustomerDue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textCustomerName = itemView.findViewById(R.id.textCustomerName);
            textCustomerDue = itemView.findViewById(R.id.textCustomerDue);
        }
    }
}
