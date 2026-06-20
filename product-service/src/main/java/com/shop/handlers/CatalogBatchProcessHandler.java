package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.model.Product;
import com.shop.repository.DynamoDbProductRepository;
import com.shop.repository.ProductRepository;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.util.Map;

public class CatalogBatchProcessHandler implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String SNS_TOPIC_ARN_ENV = "SNS_TOPIC_ARN";

    private final ProductRepository repository;
    private final SnsClient snsClient;

    public CatalogBatchProcessHandler() {
        this(new DynamoDbProductRepository(), SnsClient.create());
    }

    CatalogBatchProcessHandler(ProductRepository repository, SnsClient snsClient) {
        this.repository = repository;
        this.snsClient = snsClient;
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        context.getLogger().log("catalogBatchProcess triggered with " + event.getRecords().size() + " messages");

        String topicArn = System.getenv(SNS_TOPIC_ARN_ENV);

        for (SQSEvent.SQSMessage message : event.getRecords()) {
            String body = message.getBody();
            context.getLogger().log("Processing SQS message: " + body);
            try {
                JsonNode node = MAPPER.readTree(body);

                if (!node.hasNonNull("title") || node.get("title").asText().isBlank()) {
                    context.getLogger().log("Skipping message - missing or blank title: " + body);
                    continue;
                }
                if (!node.hasNonNull("price") || node.get("price").asDouble() <= 0) {
                    context.getLogger().log("Skipping message - invalid price: " + body);
                    continue;
                }
                if (!node.hasNonNull("count") || node.get("count").asInt() < 0) {
                    context.getLogger().log("Skipping message - invalid count: " + body);
                    continue;
                }

                Product input = new Product(
                    null,
                    node.get("title").asText(),
                    node.hasNonNull("description") ? node.get("description").asText() : "",
                    node.get("price").asDouble(),
                    node.get("count").asInt(),
                    node.hasNonNull("image") ? node.get("image").asText() : null
                );

                Product created = repository.create(input);
                context.getLogger().log("Created product: " + created.id() + " - " + created.title());

                snsClient.publish(PublishRequest.builder()
                    .topicArn(topicArn)
                    .subject("New Product Created")
                    .message("Product created: " + created.title()
                        + "\nId: " + created.id()
                        + "\nPrice: " + created.price()
                        + "\nCount: " + created.count()
                        + "\nDescription: " + created.description())
                    .messageAttributes(Map.of(
                        "price", MessageAttributeValue.builder()
                            .dataType("Number")
                            .stringValue(String.valueOf(created.price()))
                            .build()
                    ))
                    .build());
            } catch (Exception e) {
                context.getLogger().log("ERROR processing message: " + e.getMessage() + " | body: " + body);
            }
        }

        return null;
    }
}
