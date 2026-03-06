package com.shiv.shop;

public class RecentBill {
    public String billNumber, date, customerName;
    public double total;

    public RecentBill(String billNumber, String date, String customerName, double total) {
        this.billNumber = billNumber;
        this.date = date;
        this.customerName = customerName;
        this.total = total;
    }
}
