
package dev.paraplan.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Execution(ExecutionMode.CONCURRENT)
class ParaPlanIntegrationIT {

    static final String IMAGE = "postgres:16-alpine";

    @Container
    @ServiceConnection // <-- Spring Boot 3.1+ will auto-wire this as the primary DataSource
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(IMAGE)
            .withDatabaseName("paraplan")
            .withUsername("paraplan")
            .withPassword("paraplan")
            .withInitScript("init.sql");

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Test
    @DisplayName("Analyze 100 varied SQL statements via /api/analyze")
    void analyzeHundredSql() {
        List<String> sqls = generateSqls(100);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<ResponseEntity<String>> responses = sqls.parallelStream()
                .map(sql -> {
                    try {
                        Map<String, Object> payload = Map.of(
                                "sql", sql,
                                "options", Map.of("enableLandscape", true, "enableDcc", true, "mcSamples", 30)
                        );
                        HttpEntity<Map<String,Object>> req = new HttpEntity<>(payload, headers);

                        ResponseEntity<String> resp = rest
                                .postForEntity("http://localhost:" + port + "/api/analyze", req, String.class);

                        assertThat(resp.getStatusCode().is2xxSuccessful())
                                .withFailMessage("Non-2xx for SQL: %s, body: %s", sql, resp.getBody())
                                .isTrue();
                        assertThat(resp.getBody()).contains("features");
                        return resp;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed for SQL: " + sql, e);
                    }
                })
                .collect(Collectors.toList());

        assertThat(responses).hasSize(100);
    }

    static List<String> generateSqls(int n) {
        List<String> emailDomains = List.of("%@example.com", "%@mail.local", "foo%@example.com");
        List<String> orders = List.of("o.created_at DESC", "o.amount DESC", "o.amount ASC");
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        List<String> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int days = rnd.nextInt(7, 121);
            String domain = emailDomains.get(rnd.nextInt(emailDomains.size()));
            String order = orders.get(rnd.nextInt(orders.size()));
            int limit = rnd.nextInt(50, 301);

            int variant = rnd.nextInt(6);
            String sql;
            switch (variant) {
                case 0 -> sql = String.format("SELECT o.id, o.created_at, c.name FROM orders o JOIN customers c ON c.id=o.customer_id WHERE c.email ILIKE '%s' AND o.created_at >= now() - interval '%d days' ORDER BY %s LIMIT %d", domain, days, order, limit);
                case 1 -> sql = String.format("SELECT c.id, c.email, COUNT(o.id) cnt, SUM(o.amount) total FROM customers c LEFT JOIN orders o ON o.customer_id=c.id WHERE c.email ILIKE '%s' GROUP BY c.id, c.email HAVING SUM(o.amount) > %d ORDER BY total DESC LIMIT %d", domain, rnd.nextInt(500, 3000), limit);
                case 2 -> sql = String.format("SELECT o.* FROM orders o WHERE o.created_at BETWEEN now() - interval '%d days' AND now() ORDER BY %s LIMIT %d", days, order, limit);
                case 3 -> sql = String.format("SELECT o.id FROM orders o WHERE EXISTS (SELECT 1 FROM customers c WHERE c.id=o.customer_id AND c.email ILIKE '%s') ORDER BY o.id DESC LIMIT %d", domain, limit);
                case 4 -> sql = String.format("SELECT c.id, c.name FROM customers c WHERE c.name ILIKE 'User %%' AND c.id IN ( SELECT o.customer_id FROM orders o WHERE o.created_at >= now() - interval '%d days' ) ORDER BY c.id DESC LIMIT %d", days, limit);
                default -> sql = String.format("SELECT o.customer_id, avg(o.amount) avg_a FROM orders o WHERE o.created_at >= now() - interval '%d days' GROUP BY o.customer_id ORDER BY avg_a DESC LIMIT %d", days, limit);
            }
            list.add(sql);
        }
        return list;
    }
}
