package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.repository.DynamoDbProductRepository;
import com.shop.repository.ProductRepository;
import com.shop.model.Product;

import java.util.Map;

public class GetProductsByIdHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, String> HEADERS = Map.of(
        "Content-Type", "application/json",
        "Access-Control-Allow-Origin", "*"
    );

    private final ProductRepository repository;

    public GetProductsByIdHandler() {
        this(new DynamoDbProductRepository());
    }

    GetProductsByIdHandler(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map<String, String> pathParams = request.getPathParameters();
            String productId = pathParams != null ? pathParams.get("productId") : null;

            if (productId == null || productId.isBlank()) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(HEADERS)
                    .withBody("{\"message\":\"productId is required\"}");
            }

            Product product = repository.getById(productId);

            if (product == null) {
                return new APIGatewayProxyResponseEvent()
                    .withStatusCode(404)
                    .withHeaders(HEADERS)
                    .withBody("{\"message\":\"Product not found\"}");
            }

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(HEADERS)
                .withBody(MAPPER.writeValueAsString(product));
        } catch (Exception e) {
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(HEADERS)
                .withBody("{\"message\":\"Internal Server Error\"}");
        }
    }
}
