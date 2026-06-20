package com.shop.model;

public record Product(String id, String title, String description, double price, int count, String image) {

    public static final String DEFAULT_IMAGE = "https://via.placeholder.com/300x200?text=No+Image";

    public Product {
        if (image == null || image.isBlank()) {
            image = DEFAULT_IMAGE;
        }
    }

    public Product(String id, String title, String description, double price, int count) {
        this(id, title, description, price, count, null);
    }
}
