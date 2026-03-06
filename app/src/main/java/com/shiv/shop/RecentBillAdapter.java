package com.shiv.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RecentBillAdapter extends RecyclerView.Adapter<RecentBillAdapter.ViewHolder> {

    private List<RecentBill> billList;

    public RecentBillAdapter(List<RecentBill> billList) {
        this.billList = billList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textBillNo, textDate, textCustomer, textTotal;

        public ViewHolder(View itemView) {
            super(itemView);
            textBillNo = itemView.findViewById(R.id.textBillNo);
            textDate = itemView.findViewById(R.id.textBillDate);
            textCustomer = itemView.findViewById(R.id.textCustomerName);
            textTotal = itemView.findViewById(R.id.textTotalAmount);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_bill, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecentBill bill = billList.get(position);
        holder.textBillNo.setText("Bill No: #" + bill.billNumber);
        holder.textDate.setText("Date: " + bill.date);
        holder.textCustomer.setText("Customer: " + bill.customerName);
        holder.textTotal.setText("Total: ₹" + bill.total);
    }

    @Override
    public int getItemCount() {
        return billList.size();
    }
}