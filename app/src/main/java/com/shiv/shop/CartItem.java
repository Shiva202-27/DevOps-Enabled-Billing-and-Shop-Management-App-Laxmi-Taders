package com.shiv.shop;

import java.util.Locale;

public class CartItem {
    private String name;
    private String marathiName;
    private double price;
    private double quantity;
    private String unit;
    private String category;
    private double originalPrice;

    public CartItem(String name, String marathiName, double price, double quantity, String unit, String category,double originalPrice) {
        this.name = name;
        this.marathiName = marathiName;
        this.price = price;
        this.quantity = quantity;
        this.unit = unit;
        this.category = category;
        this.originalPrice = originalPrice;
    }
    public double getOriginalPrice() {
        return originalPrice;
    }

    public String getName() { return name; }
    public String getMarathiName() { return marathiName; }
    public double getRate() { return price; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public String getCategory() { return category; }

    public void setPrice(double price) { this.price = price; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public double getTotal() { return price * quantity; }

    private boolean isChecked;


}
