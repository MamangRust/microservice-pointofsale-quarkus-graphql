package com.sanedge.statsreader.cache;

import com.sanedge.common.cache.StatsCache;
import com.sanedge.common.clickhouse.ClickHouseClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Cache-aside wrapper for ClickHouse stats queries.
 * All handlers delegate to this service to get automatic Redis caching.
 * 
 * <p>Flow:
 * <ol>
 *   <li>Check Redis for cache key (apigw:stats:{type}:{sha1(sql)})</li>
 *   <li>Cache hit → deserialize and return</li>
 *   <li>Cache miss → query ClickHouse → cache result → return</li>
 * </ol>
 * 
 * <p>Fail-open: Redis errors never block or fail the API.
 */
@ApplicationScoped
public class StatsQueryService {
    private static final Logger log = LoggerFactory.getLogger(StatsQueryService.class);

    @Inject
    StatsCache cache;

    @Inject
    ClickHouseClient clickhouse;

    @ConfigProperty(name = "stats.cache.ttl-seconds", defaultValue = "300")
    long cacheTtlSeconds;

    /**
     * Execute a ClickHouse query with Redis cache-aside.
     * @param type  cache namespace type (e.g. "cashier_total_sales", "order_revenue")
     * @param sql   the ClickHouse SQL query
     * @return cached or fresh query results as JsonArray
     */
    public Uni<JsonArray> query(String type, String sql) {
        String key = cache.cacheKey(type, sql);
        return cache.get(key)
                .chain(cached -> {
                    if (cached.isPresent()) {
                        log.debug("Cache HIT for type={} key={}", type, key);
                        return Uni.createFrom().item(new JsonArray(cached.get()));
                    }
                    log.debug("Cache MISS for type={}", type);
                    return Uni.createFrom().completionStage(() -> clickhouse.query(sql))
                            .call(result -> {
                                if (result != null && !result.isEmpty()) {
                                    return cache.put(key, result.encode(), cacheTtlSeconds);
                                }
                                return Uni.createFrom().voidItem();
                            });
                })
                .onFailure().recoverWithItem(e -> {
                    log.error("StatsQuery failed (fail-open): type={} error={}", type, e.getMessage());
                    return new JsonArray();
                });
    }
}
