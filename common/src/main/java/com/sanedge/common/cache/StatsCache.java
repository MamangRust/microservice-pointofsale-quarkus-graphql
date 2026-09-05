package com.sanedge.common.cache;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

/**
 * Cache-aside helper for stats queries.
 * Uses namespace {@code apigw:stats:} to avoid collisions with domain caches.
 * 
 * <p>Payment_quarkus F6 lesson: namespace gateway cache separately from domain cache
 * to prevent field corruption (ApiResponse field mismatch → 500).
 */
@ApplicationScoped
public class StatsCache {
    private static final Logger log = LoggerFactory.getLogger(StatsCache.class);
    private static final String PREFIX = "apigw:stats:";

    @Inject
    ReactiveRedisDataSource redis;

    private ReactiveValueCommands<String, String> valueCommands;

    @jakarta.annotation.PostConstruct
    void init() {
        this.valueCommands = redis.value(String.class, String.class);
    }

    /**
     * Build a cache key from query type and SQL text.
     * Key = PREFIX + type + ":" + SHA1(sql)
     */
    public String cacheKey(String type, String sql) {
        return PREFIX + type + ":" + sha1(sql);
    }

    /**
     * Get cached value. Returns empty on miss or Redis error (fail-open).
     */
    public Uni<Optional<String>> get(String key) {
        return valueCommands.get(key)
                .onFailure().recoverWithItem(e -> {
                    log.warn("StatsCache GET failed (fail-open): {}", e.getMessage());
                    return null;
                })
                .map(v -> Optional.ofNullable(v));
    }

    /**
     * Put value with TTL. Fail-open on Redis error.
     */
    public Uni<Void> put(String key, String value, long ttlSeconds) {
        return valueCommands.setex(key, ttlSeconds, value)
                .onFailure().recoverWithItem(e -> {
                    log.warn("StatsCache PUT failed (fail-open): {}", e.getMessage());
                    return null;
                })
                .replaceWithVoid();
    }

    /**
     * Compute SHA-1 hash of input string.
     */
    private static String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 not available", e);
        }
    }
}
