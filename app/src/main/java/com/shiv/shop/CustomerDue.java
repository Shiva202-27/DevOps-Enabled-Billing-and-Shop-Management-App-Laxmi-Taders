package com.shiv.shop;

public class CustomerDue {
    private long customerId;
    private String customerName;
    private double dueAmount;

    public CustomerDue(long customerId, String customerName, double dueAmount) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.dueAmount = dueAmount;
    }

    public long getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public double getDueAmount() { return dueAmount; }
}
