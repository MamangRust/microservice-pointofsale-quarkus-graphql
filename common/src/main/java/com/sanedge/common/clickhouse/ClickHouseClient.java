package com.sanedge.common.clickhouse;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Lightweight ClickHouse HTTP client — plain class, no CDI.
 * Shared by stats-writer and stats-reader modules.
 *
 * <p>Lessons learned (payment_quarkus F4/F7):
 * <ul>
 *   <li>Password "none" = no password (Quarkus config empty-string guard)</li>
 *   <li>POST body must be "" (empty string), not null — ClickHouse rejects null</li>
 *   <li>DateTime must be yyyy-MM-dd HH:mm:ss (no T/Z)</li>
 *   <li>r.headers() must be called before r.send() to avoid OTel NPE</li>
 * </ul>
 */
public class ClickHouseClient {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseClient.class);
    private static final DateTimeFormatter CH_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WebClient client;
    private final String host;
    private final int httpPort;
    private final String database;
    private final String username;
    private final String password;

    public ClickHouseClient(Vertx vertx, String host, int httpPort, String database,
                            String username, String password) {
        this.client = WebClient.create(vertx);
        this.host = host;
        this.httpPort = httpPort;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    /**
     * Normalize a DateTime value to ClickHouse-compatible format.
     * Accepts ISO (with T/Z) or already-formatted strings.
     */
    public static String normalizeDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // If already in CH format, return as-is
        if (value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return value;
        }
        try {
            // Parse ISO format (2024-01-05T10:00:00Z or similar)
            LocalDateTime ldt = LocalDateTime.parse(value.replace("Z", ""));
            return ldt.format(CH_DATETIME);
        } catch (Exception e) {
            log.warn("Failed to normalize DateTime: {} — returning as-is", value);
            return value;
        }
    }

    /**
     * Execute a SQL statement (no result set expected).
     */
    public CompletableFuture<Void> execute(String sql) {
        String url = buildUrl(database) + "&query=" + urlEncode(sql);
        return client.postAbs(url)
                .putHeader("Content-Type", "text/plain")
                .sendBuffer(io.vertx.core.buffer.Buffer.buffer(""))
                .onSuccess(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException("ClickHouse execute failed: " + resp.bodyAsString());
                    }
                })
                .map(resp -> (Void) null)
                .toCompletionStage()
                .toCompletableFuture();
    }

    /**
     * Execute SQL without specifying a database (for CREATE DATABASE).
     */
    public CompletableFuture<Void> executeNoDatabase(String sql) {
        String url = "http://" + host + ":" + httpPort + "/?" + auth() + "&query=" + urlEncode(sql);
        return client.postAbs(url)
                .putHeader("Content-Type", "text/plain")
                .sendBuffer(io.vertx.core.buffer.Buffer.buffer(""))
                .onSuccess(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException("ClickHouse executeNoDatabase failed: " + resp.bodyAsString());
                    }
                })
                .map(resp -> (Void) null)
                .toCompletionStage()
                .toCompletableFuture();
    }

    /**
     * Insert a batch of rows using FORMAT JSONEachRow.
     */
    public CompletableFuture<Void> insert(String table, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        JsonArray jsonArray = new JsonArray();
        for (Map<String, Object> row : rows) {
            jsonArray.add(new JsonObject(row));
        }

        String sql = "INSERT INTO " + table + " FORMAT JSONEachRow";
        String url = buildUrl(database) + "&query=" + urlEncode(sql);

        return client.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .sendBuffer(io.vertx.core.buffer.Buffer.buffer(jsonArray.encode()))
                .onSuccess(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException("ClickHouse insert failed: " + resp.bodyAsString());
                    }
                })
                .map(resp -> (Void) null)
                .toCompletionStage()
                .toCompletableFuture();
    }

    /**
     * Query and return results as a JsonArray.
     */
    public CompletableFuture<JsonArray> query(String sql) {
        String url = buildUrl(database) + "&query=" + urlEncode(sql);
        return client.postAbs(url)
                .putHeader("Content-Type", "text/plain")
                .sendBuffer(io.vertx.core.buffer.Buffer.buffer(""))
                .map(resp -> {
                    if (resp.statusCode() != 200) {
                        throw new RuntimeException("ClickHouse query failed: " + resp.bodyAsString());
                    }
                    return new JsonArray(resp.bodyAsString());
                })
                .toCompletionStage()
                .toCompletableFuture();
    }

    private String buildUrl(String db) {
        StringBuilder sb = new StringBuilder("http://").append(host).append(":").append(httpPort).append("/?");
        sb.append(auth());
        if (db != null && !db.isBlank()) {
            sb.append("&database=").append(urlEncode(db));
        }
        return sb.toString();
    }

    /**
     * Build auth query parameters. Treat "none" as no-password (lesson F7).
     */
    private String auth() {
        StringBuilder sb = new StringBuilder();
        if (username != null && !username.isBlank()) {
            sb.append("user=").append(urlEncode(username));
        }
        if (password != null && !password.isBlank() && !"none".equals(password)) {
            if (sb.length() > 0) sb.append("&");
            sb.append("password=").append(urlEncode(password));
        }
        return sb.toString();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
