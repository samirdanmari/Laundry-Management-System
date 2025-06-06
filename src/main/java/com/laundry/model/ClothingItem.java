package com.laundry.model;

public class ClothingItem {
    private String type;
    private int quantity;
    private double price;
    private double totalPrice;
    
    // Default constructor for JSON deserialization
    public ClothingItem() {}
    
    // Constructor with parameters
    public ClothingItem(String type, int quantity, double price) {
        this.type = type;
        this.quantity = quantity;
        this.price = price;
        this.totalPrice = quantity * price;
    }
    
    // Getters and Setters
    public String getType() { 
        return type; 
    }
    
    public void setType(String type) { 
        this.type = type; 
    }
    
    public int getQuantity() { 
        return quantity; 
    }
    
    public void setQuantity(int quantity) { 
        this.quantity = quantity;
        this.totalPrice = quantity * this.price; // Recalculate total
    }
    
    public double getPrice() { 
        return price; 
    }
    
    public void setPrice(double price) { 
        this.price = price;
        this.totalPrice = this.quantity * price; // Recalculate total
    }
    
    public double getTotalPrice() { 
        return totalPrice; 
    }
    
    public void setTotalPrice(double totalPrice) { 
        this.totalPrice = totalPrice; 
    }
    
    // Calculate total price automatically
    public void calculateTotalPrice() {
        this.totalPrice = this.quantity * this.price;
    }
    
    @Override
    public String toString() {
        return String.format("%s (Qty: %d, Price: %.2f, Total: %.2f)", 
                           type, quantity, price, totalPrice);
    }
}

//
