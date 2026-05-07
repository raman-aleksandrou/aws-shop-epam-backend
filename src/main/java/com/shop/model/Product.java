package com.shop.model;

public class Product {
    private String id;
    private String title;
    private String description;
    private double price;
    private int count;

    public Product() {}

    public Product(String id, String title, String description, double price, int count) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
        this.count = count;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
