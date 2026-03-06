package com.shiv.shop;

public class ProductInfo {
    private String name;
    private double price;
    private String unit;

    public ProductInfo(String name, double price, String unit) {
        this.name = name;
        this.price = price;
        this.unit = unit;
    }

    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    public String getUnit() {         // ✅ This method must exist
        return unit;
    }

    @Override
    public String toString() {
        return name + " (₹" + (double) price + "/" + unit + ")";
    }
}
