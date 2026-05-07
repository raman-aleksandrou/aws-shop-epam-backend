package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GetProductsByIdHandlerTest {

    private GetProductsByIdHandler handler;
    private Context context;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new GetProductsByIdHandler();
        context = mock(Context.class);
    }

    private APIGatewayProxyRequestEvent requestWithId(String productId) {
        return new APIGatewayProxyRequestEvent()
            .withPathParameters(Map.of("productId", productId));
    }

    @Test
    void returnsStatus200ForExistingProduct() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithId("1"), context);
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void returnsCorrectProduct() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithId("1"), context);
        Product product = MAPPER.readValue(response.getBody(), Product.class);
        assertEquals("1", product.getId());
        assertEquals("ProductOne", product.getTitle());
    }

    @Test
    void returns404ForUnknownId() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithId("999"), context);
        assertEquals(404, response.getStatusCode());
    }

    @Test
    void returns404BodyMessage() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithId("999"), context);
        String message = MAPPER.readTree(response.getBody()).get("message").asText();
        assertEquals("Product not found", message);
    }

    @Test
    void returns400WhenPathParametersAreNull() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), context);
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void returns400BodyMessage() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), context);
        String message = MAPPER.readTree(response.getBody()).get("message").asText();
        assertEquals("productId is required", message);
    }

    @Test
    void responseHasJsonContentTypeHeader() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithId("1"), context);
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void responseHasCorsHeader() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithId("1"), context);
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
    }

    @Test
    void returnsAllSevenProducts() {
        for (int i = 1; i <= 7; i++) {
            APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithId(String.valueOf(i)), context);
            assertEquals(200, response.getStatusCode(), "Expected 200 for product id=" + i);
        }
    }
}
