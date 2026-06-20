package com.shop.bff;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Single-entry, time-based cache for the Product Service getProductsList response.
 * Entries expire after a configurable TTL (default 2 minutes). Only successful
 * responses should be stored, so that error responses are never served from cache.
 */
@Component
public class ProductsListCache {

    private final Duration ttl;
    private final AtomicReference<Entry> entry = new AtomicReference<>();

    public ProductsListCache(@Value("${bff.products-cache.ttl-seconds:120}") long ttlSeconds) {
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    /** Returns the cached response, or {@code null} if absent or expired. */
    public ResponseEntity<byte[]> get() {
        Entry current = entry.get();
        if (current == null || Instant.now().isAfter(current.expiresAt())) {
            return null;
        }
        return current.response();
    }

    public void put(ResponseEntity<byte[]> response) {
        entry.set(new Entry(response, Instant.now().plus(ttl)));
    }

    private record Entry(ResponseEntity<byte[]> response, Instant expiresAt) {}
}
