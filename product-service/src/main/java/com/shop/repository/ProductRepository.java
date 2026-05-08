package com.shop.repository;

import com.shop.model.Product;

import java.util.List;

public interface ProductRepository {
    List<Product> getAll();
    Product getById(String id);
    Product create(Product product);
}
