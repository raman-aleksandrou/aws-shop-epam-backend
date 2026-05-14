package com.shop.repository;

import com.shop.model.Product;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class DynamoDbProductRepository implements ProductRepository {

    private final DynamoDbClient dynamoDb;
    private final String productsTable;
    private final String stocksTable;

    public DynamoDbProductRepository() {
        this(
            DynamoDbClient.create(),
            System.getenv("PRODUCTS_TABLE_NAME"),
            System.getenv("STOCKS_TABLE_NAME")
        );
    }

    DynamoDbProductRepository(DynamoDbClient dynamoDb, String productsTable, String stocksTable) {
        this.dynamoDb = dynamoDb;
        this.productsTable = productsTable;
        this.stocksTable = stocksTable;
    }

    @Override
    public List<Product> getAll() {
        ScanResponse productsResp = dynamoDb.scan(ScanRequest.builder().tableName(productsTable).build());
        ScanResponse stocksResp = dynamoDb.scan(ScanRequest.builder().tableName(stocksTable).build());

        Map<String, Integer> stocksMap = stocksResp.items().stream()
            .collect(Collectors.toMap(
                item -> item.get("product_id").s(),
                item -> Integer.parseInt(item.get("count").n())
            ));

        return productsResp.items().stream()
            .map(item -> mapToProduct(item, stocksMap.getOrDefault(item.get("id").s(), 0)))
            .collect(Collectors.toList());
    }

    @Override
    public Product getById(String id) {
        GetItemResponse productResp = dynamoDb.getItem(GetItemRequest.builder()
            .tableName(productsTable)
            .key(Map.of("id", AttributeValue.fromS(id)))
            .build());

        if (!productResp.hasItem() || productResp.item().isEmpty()) {
            return null;
        }

        GetItemResponse stockResp = dynamoDb.getItem(GetItemRequest.builder()
            .tableName(stocksTable)
            .key(Map.of("product_id", AttributeValue.fromS(id)))
            .build());

        int count = 0;
        if (stockResp.hasItem() && stockResp.item().containsKey("count")) {
            count = Integer.parseInt(stockResp.item().get("count").n());
        }

        return mapToProduct(productResp.item(), count);
    }

    @Override
    public Product create(Product product) {
        String id = UUID.randomUUID().toString();

        TransactWriteItem putProduct = TransactWriteItem.builder()
            .put(Put.builder()
                .tableName(productsTable)
                .item(Map.of(
                    "id",          AttributeValue.fromS(id),
                    "title",       AttributeValue.fromS(product.title()),
                    "description", AttributeValue.fromS(product.description()),
                    "price",       AttributeValue.fromN(String.valueOf(product.price()))
                ))
                .build())
            .build();

        TransactWriteItem putStock = TransactWriteItem.builder()
            .put(Put.builder()
                .tableName(stocksTable)
                .item(Map.of(
                    "product_id", AttributeValue.fromS(id),
                    "count",      AttributeValue.fromN(String.valueOf(product.count()))
                ))
                .build())
            .build();

        dynamoDb.transactWriteItems(TransactWriteItemsRequest.builder()
            .transactItems(putProduct, putStock)
            .build());

        return new Product(id, product.title(), product.description(), product.price(), product.count());
    }

    private Product mapToProduct(Map<String, AttributeValue> item, int count) {
        return new Product(
            item.get("id").s(),
            item.get("title").s(),
            item.containsKey("description") ? item.get("description").s() : "",
            Double.parseDouble(item.get("price").n()),
            count
        );
    }
}
