package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GetProductsListHandlerTest {

    private GetProductsListHandler handler;
    private Context context;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new GetProductsListHandler();
        context = mock(Context.class);
    }

    @Test
    void returnsStatus200() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), context);
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void returnsAllProducts() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), context);
        List<Product> products = MAPPER.readValue(response.getBody(), new TypeReference<>() {});
        assertEquals(7, products.size());
    }

    @Test
    void responseBodyIsValidJson() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), context);
        assertDoesNotThrow(() -> MAPPER.readTree(response.getBody()));
    }

    @Test
    void responseHasJsonContentTypeHeader() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), context);
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void responseHasCorsHeader() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), context);
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
    }

    @Test
    void productsHaveRequiredFields() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), context);
        List<Product> products = MAPPER.readValue(response.getBody(), new TypeReference<>() {});
        for (Product product : products) {
            assertNotNull(product.getId());
            assertNotNull(product.getTitle());
            assertNotNull(product.getDescription());
            assertTrue(product.getPrice() > 0);
            assertTrue(product.getCount() >= 0);
        }
    }
}
