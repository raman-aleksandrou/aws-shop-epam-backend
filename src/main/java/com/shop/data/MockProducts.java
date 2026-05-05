package com.shop.data;

import com.shop.model.Product;

import java.util.List;

public class MockProducts {

    private static final List<Product> PRODUCTS = List.of(
        new Product("1", "ProductOne", "Short Product Description1", 24.0, 8),
        new Product("2", "ProductTitle", "Short Product Description7", 15.0, 3),
        new Product("3", "Product", "Short Product Description2", 23.0, 5),
        new Product("4", "ProductTest", "Short Product Description4", 15.0, 1),
        new Product("5", "ProductTitle", "Short Product Description6", 23.0, 6),
        new Product("6", "Product2", "Short Product Description3", 15.0, 7),
        new Product("7", "ProductTitle", "Short Product Description5", 15.0, 4)
    );

    public static List<Product> getAll() {
        return PRODUCTS;
    }

    public static Product getById(String id) {
        return PRODUCTS.stream()
            .filter(p -> p.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
}
