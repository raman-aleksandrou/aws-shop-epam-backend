package com.shop.bff;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves a recipient service name (e.g. "product", "cart") to the base URL
 * configured for it in the .env file. Keys are matched case-insensitively.
 */
@Component
public class ServiceUrlResolver {

    private final Map<String, String> services = new HashMap<>();

    public ServiceUrlResolver() {
        // 1. Local development: read mappings from the .env file (if present).
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry ->
                services.put(entry.getKey().toLowerCase(), entry.getValue()));

        // 2. Deployment: Elastic Beanstalk environment properties are exposed as
        //    OS environment variables and take precedence over any .env value.
        System.getenv().forEach((key, value) ->
                services.put(key.toLowerCase(), value));
    }

    public Optional<String> resolve(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(services.get(serviceName.toLowerCase()));
    }
}
