package com.shop;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.RestApiProps;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

public class ProductServiceStack extends Stack {

    public ProductServiceStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Function getProductsList = Function.Builder.create(this, "GetProductsListFunction")
            .functionName("getProductsList")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.GetProductsListHandler::handleRequest")
            .code(Code.fromAsset("target/product-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .build();

        Function getProductsById = Function.Builder.create(this, "GetProductsByIdFunction")
            .functionName("getProductsById")
            .runtime(Runtime.JAVA_17)
            .handler("com.shop.handlers.GetProductsByIdHandler::handleRequest")
            .code(Code.fromAsset("target/product-service.jar"))
            .memorySize(512)
            .timeout(Duration.seconds(15))
            .build();

        RestApi api = RestApi.Builder.create(this, "ProductServiceApi")
            .restApiName("Product Service API")
            .description("Product Service REST API")
            .build();

        var productsResource = api.getRoot().addResource("products");
        productsResource.addMethod("GET", new LambdaIntegration(getProductsList));
        productsResource.addResource("{productId}")
            .addMethod("GET", new LambdaIntegration(getProductsById));
    }
}
