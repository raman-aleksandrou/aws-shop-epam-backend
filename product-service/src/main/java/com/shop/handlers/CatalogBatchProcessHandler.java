package com.shop.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.model.Product;
import com.shop.repository.DynamoDbProductRepository;
import com.shop.repository.ProductRepository;

public class CatalogBatchProcessHandler implements RequestHandler<SQSEvent, Void> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProductRepository repository;

    public CatalogBatchProcessHandler() {
        this(new DynamoDbProductRepository());
    }

    CatalogBatchProcessHandler(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        context.getLogger().log("catalogBatchProcess triggered with " + event.getRecords().size() + " messages");

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
                    node.get("count").asInt()
                );

                Product created = repository.create(input);
                context.getLogger().log("Created product: " + created.id() + " - " + created.title());
            } catch (Exception e) {
                context.getLogger().log("ERROR processing message: " + e.getMessage() + " | body: " + body);
            }
        }

        return null;
    }
}
