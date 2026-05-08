package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.repository.ProductRepository;
import com.shop.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetProductsListHandlerTest {

    private GetProductsListHandler handler;
    private Context context;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<Product> MOCK_PRODUCTS = List.of(
        new Product("1", "ProductOne",   "Short Product Description1", 24.0, 8),
        new Product("2", "ProductTitle", "Short Product Description7", 15.0, 3),
        new Product("3", "Product",      "Short Product Description2", 23.0, 5),
        new Product("4", "ProductTest",  "Short Product Description4", 15.0, 1),
        new Product("5", "ProductTitle", "Short Product Description6", 23.0, 6),
        new Product("6", "Product2",     "Short Product Description3", 15.0, 7),
        new Product("7", "ProductTitle", "Short Product Description5", 15.0, 4)
    );

    @BeforeEach
    void setUp() {
        ProductRepository repository = mock(ProductRepository.class);
        when(repository.getAll()).thenReturn(MOCK_PRODUCTS);
        handler = new GetProductsListHandler(repository);
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
            assertNotNull(product.id());
            assertNotNull(product.title());
            assertNotNull(product.description());
            assertTrue(product.price() > 0);
            assertTrue(product.count() >= 0);
        }
    }
}
