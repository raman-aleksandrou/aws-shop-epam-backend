package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.repository.DynamoDbProductRepository;
import com.shop.repository.ProductRepository;
import com.shop.model.Product;

import java.util.List;
import java.util.Map;

public class GetProductsListHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, String> HEADERS = Map.of(
        "Content-Type", "application/json",
        "Access-Control-Allow-Origin", "*"
    );

    private final ProductRepository repository;

    public GetProductsListHandler() {
        this(new DynamoDbProductRepository());
    }

    GetProductsListHandler(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Incoming request: GET /products");
        try {
            List<Product> products = repository.getAll();
            String body = MAPPER.writeValueAsString(products);
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(HEADERS)
                .withBody(body);
        } catch (Exception e) {
            context.getLogger().log("ERROR getProductsList: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(HEADERS)
                .withBody("{\"message\":\"Internal Server Error\"}");
        }
    }
}
