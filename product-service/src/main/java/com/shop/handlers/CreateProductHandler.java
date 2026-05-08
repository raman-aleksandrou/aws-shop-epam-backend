package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.model.Product;
import com.shop.repository.DynamoDbProductRepository;
import com.shop.repository.ProductRepository;

import java.util.Map;

public class CreateProductHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, String> HEADERS = Map.of(
        "Content-Type", "application/json",
        "Access-Control-Allow-Origin", "*"
    );

    private final ProductRepository repository;

    public CreateProductHandler() {
        this(new DynamoDbProductRepository());
    }

    CreateProductHandler(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        context.getLogger().log("Incoming request: POST /products, body: " + request.getBody());
        try {
            String body = request.getBody();
            if (body == null || body.isBlank()) {
                return badRequest("Request body is required");
            }

            JsonNode node;
            try {
                node = MAPPER.readTree(body);
            } catch (JsonParseException e) {
                return badRequest("Request body is not valid JSON");
            }

            if (!node.hasNonNull("title") || node.get("title").asText().isBlank()) {
                return badRequest("title is required");
            }
            if (!node.hasNonNull("price") || node.get("price").asDouble() <= 0) {
                return badRequest("price must be a positive number");
            }
            if (!node.hasNonNull("count") || node.get("count").asInt() < 0) {
                return badRequest("count must be a non-negative integer");
            }

            Product input = new Product(
                null,
                node.get("title").asText(),
                node.hasNonNull("description") ? node.get("description").asText() : "",
                node.get("price").asDouble(),
                node.get("count").asInt()
            );

            Product created = repository.create(input);

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(201)
                .withHeaders(HEADERS)
                .withBody(MAPPER.writeValueAsString(created));
        } catch (Exception e) {
            context.getLogger().log("ERROR createProduct: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                .withStatusCode(500)
                .withHeaders(HEADERS)
                .withBody("{\"message\":\"Internal Server Error\"}");
        }
    }

    private APIGatewayProxyResponseEvent badRequest(String message) {
        return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withHeaders(HEADERS)
            .withBody("{\"message\":\"" + message + "\"}");
    }
}
