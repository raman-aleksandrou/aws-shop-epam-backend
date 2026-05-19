package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.shop.model.Product;
import com.shop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CatalogBatchProcessHandlerTest {

    private CatalogBatchProcessHandler handler;
    private ProductRepository repository;
    private SnsClient snsClient;
    private Context context;

    @BeforeEach
    void setUp() {
        repository = mock(ProductRepository.class);
        when(repository.create(any())).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return new Product("generated-uuid", p.title(), p.description(), p.price(), p.count());
        });
        snsClient = mock(SnsClient.class);
        when(snsClient.publish(any(PublishRequest.class)))
            .thenReturn(PublishResponse.builder().messageId("msg-id").build());
        handler = new CatalogBatchProcessHandler(repository, snsClient);
        context = mock(Context.class);
        when(context.getLogger()).thenReturn(mock(LambdaLogger.class));
    }

    private SQSEvent sqsEvent(String... bodies) {
        SQSEvent event = new SQSEvent();
        event.setRecords(java.util.Arrays.stream(bodies).map(body -> {
            SQSEvent.SQSMessage msg = new SQSEvent.SQSMessage();
            msg.setBody(body);
            return msg;
        }).toList());
        return event;
    }

    @Test
    void createsProductForEachValidMessage() {
        SQSEvent event = sqsEvent(
            "{\"title\":\"Product A\",\"price\":10.0,\"count\":5}",
            "{\"title\":\"Product B\",\"price\":20.0,\"count\":3}"
        );

        handler.handleRequest(event, context);

        verify(repository, times(2)).create(any());
    }

    @Test
    void publishesToSnsForEachCreatedProduct() {
        SQSEvent event = sqsEvent(
            "{\"title\":\"Product A\",\"price\":10.0,\"count\":5}",
            "{\"title\":\"Product B\",\"price\":20.0,\"count\":3}"
        );

        handler.handleRequest(event, context);

        verify(snsClient, times(2)).publish(any(PublishRequest.class));
    }

    @Test
    void snsPublishIncludesPriceMessageAttribute() {
        SQSEvent event = sqsEvent(
            "{\"title\":\"My Product\",\"price\":49.99,\"count\":10}"
        );

        handler.handleRequest(event, context);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        var priceAttr = captor.getValue().messageAttributes().get("price");
        assertNotNull(priceAttr);
        assertEquals("Number", priceAttr.dataType());
        assertEquals("49.99", priceAttr.stringValue());
    }

    @Test
    void snsMessageContainsProductDetails() {
        SQSEvent event = sqsEvent(
            "{\"title\":\"My Product\",\"description\":\"Desc\",\"price\":49.99,\"count\":10}"
        );

        handler.handleRequest(event, context);

        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        String message = captor.getValue().message();
        assertTrue(message.contains("My Product"));
        assertTrue(message.contains("generated-uuid"));
        assertEquals("New Product Created", captor.getValue().subject());
    }

    @Test
    void createsProductWithCorrectFields() {
        SQSEvent event = sqsEvent(
            "{\"title\":\"My Product\",\"description\":\"Desc\",\"price\":49.99,\"count\":10}"
        );

        handler.handleRequest(event, context);

        verify(repository).create(new Product(null, "My Product", "Desc", 49.99, 10));
    }

    @Test
    void usesEmptyDescriptionWhenMissing() {
        SQSEvent event = sqsEvent(
            "{\"title\":\"No Desc\",\"price\":5.0,\"count\":1}"
        );

        handler.handleRequest(event, context);

        verify(repository).create(new Product(null, "No Desc", "", 5.0, 1));
    }

    @Test
    void skipsMessageWithMissingTitle() {
        SQSEvent event = sqsEvent("{\"price\":10.0,\"count\":1}");

        handler.handleRequest(event, context);

        verify(repository, never()).create(any());
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void skipsMessageWithBlankTitle() {
        SQSEvent event = sqsEvent("{\"title\":\"  \",\"price\":10.0,\"count\":1}");

        handler.handleRequest(event, context);

        verify(repository, never()).create(any());
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void skipsMessageWithZeroPrice() {
        SQSEvent event = sqsEvent("{\"title\":\"T\",\"price\":0,\"count\":1}");

        handler.handleRequest(event, context);

        verify(repository, never()).create(any());
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void skipsMessageWithNegativePrice() {
        SQSEvent event = sqsEvent("{\"title\":\"T\",\"price\":-5.0,\"count\":1}");

        handler.handleRequest(event, context);

        verify(repository, never()).create(any());
    }

    @Test
    void skipsMessageWithNegativeCount() {
        SQSEvent event = sqsEvent("{\"title\":\"T\",\"price\":10.0,\"count\":-1}");

        handler.handleRequest(event, context);

        verify(repository, never()).create(any());
    }

    @Test
    void skipsInvalidJsonMessage() {
        SQSEvent event = sqsEvent("not-json");

        handler.handleRequest(event, context);

        verify(repository, never()).create(any());
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void processesValidMessagesAndSkipsInvalidOnesInSameBatch() {
        SQSEvent event = sqsEvent(
            "{\"title\":\"Valid\",\"price\":10.0,\"count\":1}",
            "{\"price\":10.0,\"count\":1}",
            "{\"title\":\"Also Valid\",\"price\":5.0,\"count\":2}"
        );

        handler.handleRequest(event, context);

        verify(repository, times(2)).create(any());
        verify(snsClient, times(2)).publish(any(PublishRequest.class));
    }

    @Test
    void returnsNullAsRequired() {
        SQSEvent event = sqsEvent("{\"title\":\"T\",\"price\":1.0,\"count\":0}");

        Void result = handler.handleRequest(event, context);

        assertNull(result);
    }

    @Test
    void handlesEmptyBatch() {
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of());

        handler.handleRequest(event, context);

        verify(repository, never()).create(any());
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void continuesProcessingAfterRepositoryException() {
        doThrow(new RuntimeException("DynamoDB error"))
            .doAnswer(inv -> {
                Product p = inv.getArgument(0);
                return new Product("uuid", p.title(), p.description(), p.price(), p.count());
            })
            .when(repository).create(any());

        SQSEvent event = sqsEvent(
            "{\"title\":\"First\",\"price\":10.0,\"count\":1}",
            "{\"title\":\"Second\",\"price\":20.0,\"count\":2}"
        );

        handler.handleRequest(event, context);

        verify(repository, times(2)).create(any());
    }
}
