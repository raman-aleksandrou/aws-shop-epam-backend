package com.shop;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.ITable;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.Map;

public class ProductServiceStack extends Stack {

    private static final String PRODUCTS_TABLE_NAME = "products";
    private static final String STOCKS_TABLE_NAME = "stocks";

    public ProductServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        ITable productsTable = Table.fromTableName(this, "ProductsTable", PRODUCTS_TABLE_NAME);
        ITable stocksTable = Table.fromTableName(this, "StocksTable", STOCKS_TABLE_NAME);

        Map<String, String> env = Map.of(
            "PRODUCTS_TABLE_NAME", PRODUCTS_TABLE_NAME,
            "STOCKS_TABLE_NAME", STOCKS_TABLE_NAME
        );

        Function getProductsList = Function.Builder.create(this, "GetProductsListFunction")
            .functionName("getProductsList")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.GetProductsListHandler::handleRequest")
            .code(Code.fromAsset("target/product-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .environment(env)
            .build();

        Function getProductsById = Function.Builder.create(this, "GetProductsByIdFunction")
            .functionName("getProductsById")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.GetProductsByIdHandler::handleRequest")
            .code(Code.fromAsset("target/product-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .environment(env)
            .build();

        Function createProduct = Function.Builder.create(this, "CreateProductFunction")
            .functionName("createProduct")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.CreateProductHandler::handleRequest")
            .code(Code.fromAsset("target/product-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .environment(env)
            .build();

        productsTable.grantReadData(getProductsList);
        stocksTable.grantReadData(getProductsList);
        productsTable.grantReadData(getProductsById);
        stocksTable.grantReadData(getProductsById);
        productsTable.grantWriteData(createProduct);
        stocksTable.grantWriteData(createProduct);

        RestApi api = RestApi.Builder.create(this, "ProductServiceApi")
            .restApiName("Product Service API")
            .description("Product Service REST API")
            .build();

        var productsResource = api.getRoot().addResource("products");
        productsResource.addMethod("GET", new LambdaIntegration(getProductsList));
        productsResource.addMethod("POST", new LambdaIntegration(createProduct));
        productsResource.addResource("{productId}")
            .addMethod("GET", new LambdaIntegration(getProductsById));
    }
}
