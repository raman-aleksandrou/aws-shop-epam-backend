package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.model.Product;
import com.shop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateProductHandlerTest {

    private CreateProductHandler handler;
    private Context context;
    private ProductRepository repository;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String VALID_BODY = """
        {"title":"Test Product","description":"A description","price":99.99,"count":5}
        """;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        when(repository.create(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return new Product("generated-uuid", p.title(), p.description(), p.price(), p.count());
        });
        handler = new CreateProductHandler(repository);
        context = mock(Context.class);
        when(context.getLogger()).thenReturn(mock(LambdaLogger.class));
    }

    private APIGatewayProxyRequestEvent requestWithBody(String body) {
        return new APIGatewayProxyRequestEvent().withBody(body);
    }

    @Test
    void returns201ForValidRequest() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithBody(VALID_BODY), context);
        assertEquals(201, response.getStatusCode());
    }

    @Test
    void responseContainsCreatedProduct() throws Exception {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithBody(VALID_BODY), context);
        Product product = MAPPER.readValue(response.getBody(), Product.class);
        assertEquals("generated-uuid", product.id());
        assertEquals("Test Product", product.title());
        assertEquals(99.99, product.price());
        assertEquals(5, product.count());
    }

    @Test
    void returns400WhenBodyIsNull() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(new APIGatewayProxyRequestEvent(), context);
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void returns400WhenTitleMissing() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
            requestWithBody("{\"price\":10,\"count\":1}"), context);
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void returns400WhenPriceIsZero() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
            requestWithBody("{\"title\":\"T\",\"price\":0,\"count\":1}"), context);
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void returns400WhenBodyIsInvalidJson() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
            requestWithBody("not-json"), context);
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void returns400WhenCountIsNegative() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(
            requestWithBody("{\"title\":\"T\",\"price\":10,\"count\":-1}"), context);
        assertEquals(400, response.getStatusCode());
    }

    @Test
    void responseHasJsonContentTypeHeader() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithBody(VALID_BODY), context);
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
    }

    @Test
    void responseHasCorsHeader() {
        APIGatewayProxyResponseEvent response = handler.handleRequest(requestWithBody(VALID_BODY), context);
        assertEquals("*", response.getHeaders().get("Access-Control-Allow-Origin"));
    }
}
