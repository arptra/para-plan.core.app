
package dev.paraplan.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExtendedSqlCoverageIT {

    static final String IMAGE = "postgres:16-alpine";

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>(IMAGE)
            .withDatabaseName("paraplan")
            .withUsername("paraplan")
            .withPassword("paraplan")
            .withInitScript("init.sql");

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String createConnection() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> payload = Map.of(
                "host", PG.getHost(),
                "port", PG.getMappedPort(5432),
                "database", PG.getDatabaseName(),
                "user", PG.getUsername(),
                "password", PG.getPassword()
        );
        ResponseEntity<Map> resp = rest.postForEntity("http://localhost:" + port + "/connections",
                new HttpEntity<>(payload, headers), Map.class);
        return (String) resp.getBody().get("id");
    }

    @Test
    @DisplayName("Cover broad SQL surface (SELECT/DML/CTE/UNION/WINDOW/CASE/etc) via EXPLAIN")
    void coverBroadSql() {
        List<String> sqls = statements();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String connId = createConnection();

        for (String sql: sqls) {
            Map<String, Object> payload = Map.of(
                    "sql", sql,
                    "connectionId", connId,
                    "schema", "public",
                    "options", Map.of("enableLandscape", true, "enableDcc", true, "mcSamples", 25)
            );
            ResponseEntity<String> resp = rest.postForEntity("http://localhost:" + port + "/api/analyze",
                    new HttpEntity<>(payload, headers), String.class);

            assertThat(resp.getStatusCode().is2xxSuccessful())
                    .withFailMessage("Non-2xx for SQL: %s, body: %s", sql, resp.getBody())
                    .isTrue();
            assertThat(resp.getBody()).contains("\"features\"");
        }
    }

    // Produce diverse EXPLAIN-able statements. We avoid DDL; Postgres supports EXPLAIN for DML as well.
    static List<String> statements() {
        List<String> s = new ArrayList<>();

        // Simple SELECT with WHERE, ORDER, LIMIT/OFFSET
        s.add("SELECT id, name FROM customers WHERE email ILIKE '%@example.com' ORDER BY id DESC LIMIT 50 OFFSET 10");

        // JOIN variants
        s.add("SELECT o.id, c.name FROM orders o INNER JOIN customers c ON c.id=o.customer_id WHERE o.created_at >= now() - interval '14 days'");
        s.add("SELECT o.id, c.name FROM orders o LEFT JOIN customers c ON c.id=o.customer_id ORDER BY o.id DESC LIMIT 100");
        s.add("SELECT * FROM customers c RIGHT JOIN orders o ON o.customer_id=c.id WHERE c.region_id = 1 LIMIT 100");
        s.add("SELECT * FROM customers c FULL JOIN orders o ON o.customer_id=c.id WHERE (c.id IS NULL OR o.id IS NULL) LIMIT 100");
        s.add("SELECT * FROM customers c CROSS JOIN regions r WHERE r.id = c.region_id AND c.id < 100");

        // LATERAL join
        s.add("SELECT c.id, x.total FROM customers c LEFT JOIN LATERAL (SELECT SUM(oi.qty*i.price) total FROM orders o JOIN order_items oi ON oi.order_id=o.id JOIN items i ON i.id=oi.item_id WHERE o.customer_id=c.id LIMIT 1) x ON true WHERE c.id < 500");

        // CTE / WITH
        s.add("WITH recent AS (SELECT * FROM orders WHERE created_at > now() - interval '7 days') SELECT r.customer_id, COUNT(*) FROM recent r GROUP BY r.customer_id HAVING COUNT(*)>1 ORDER BY COUNT(*) DESC LIMIT 50");

        // Subquery EXISTS/IN/ANY/ALL
        s.add("SELECT id FROM customers c WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id=c.id AND o.amount > 100)");
        s.add("SELECT id FROM customers WHERE id IN (SELECT customer_id FROM orders WHERE status='PAID' LIMIT 100)");
        s.add("SELECT id FROM customers WHERE id = ANY (SELECT customer_id FROM orders WHERE amount > 500)");
        s.add("SELECT id FROM customers WHERE 1000 > ALL (SELECT amount FROM orders WHERE orders.customer_id = customers.id)");

        // Aggregation, GROUP BY, HAVING
        s.add("SELECT c.region_id, COUNT(*) cnt, SUM(o.amount) sum_a FROM customers c JOIN orders o ON o.customer_id=c.id GROUP BY c.region_id HAVING SUM(o.amount) > 1000 ORDER BY sum_a DESC");

        // Window functions
        s.add("SELECT o.id, o.customer_id, o.amount, rank() OVER (PARTITION BY o.customer_id ORDER BY o.amount DESC) rnk FROM orders o WHERE o.created_at >= now() - interval '30 days'");

        // DISTINCT, DISTINCT ON
        s.add("SELECT DISTINCT c.region_id FROM customers c");
        s.add("SELECT DISTINCT ON (o.customer_id) o.customer_id, o.id, o.amount FROM orders o ORDER BY o.customer_id, o.amount DESC");

        // UNION / INTERSECT / EXCEPT
        s.add("SELECT id FROM customers WHERE region_id=1 UNION ALL SELECT id FROM customers WHERE region_id=2");
        s.add("SELECT customer_id FROM orders WHERE amount>500 INTERSECT SELECT customer_id FROM orders WHERE status='PAID'");
        s.add("SELECT customer_id FROM orders EXCEPT SELECT customer_id FROM orders WHERE status='CANCELLED'");

        // CASE expressions
        s.add("SELECT id, CASE WHEN amount>1000 THEN 'high' WHEN amount>100 THEN 'mid' ELSE 'low' END AS bucket FROM orders ORDER BY id DESC LIMIT 200");

        // COALESCE / NULLIF / CAST
        s.add("SELECT CAST(id AS BIGINT), COALESCE(name,'N/A') FROM customers WHERE NULLIF(email,'') IS NOT NULL LIMIT 100");

        // LIKE/ILIKE / SIMILAR TO / BETWEEN
        s.add("SELECT * FROM customers WHERE name SIMILAR TO '(User|Admin) %' AND id BETWEEN 100 AND 200");
        s.add("SELECT * FROM orders WHERE status ILIKE 'PAI%' AND amount BETWEEN 50 AND 150 ORDER BY created_at DESC LIMIT 100");

        // ARRAY, ANY element, unnest
        s.add("SELECT i.id FROM items i WHERE 'tag1' = ANY(i.tags)");
        s.add("SELECT i.id, t FROM items i, unnest(i.tags) AS t WHERE t LIKE 'grp%'");

        // JSON building (expression only; still a SELECT)
        s.add("SELECT json_build_object('id', c.id, 'email', c.email) FROM customers c WHERE c.id < 50");

        // View usage
        s.add("SELECT * FROM v_order_value WHERE calc_amount > 500 ORDER BY calc_amount DESC LIMIT 50");

        // ORDER BY with NULLS FIRST/LAST
        s.add("SELECT c.id, c.region_id FROM customers c ORDER BY c.region_id NULLS LAST, c.id ASC LIMIT 100");

        // OFFSET/FETCH syntax
        s.add("SELECT id FROM orders ORDER BY id FETCH FIRST 100 ROWS ONLY");

        // DML (EXPLAIN supports): INSERT/UPDATE/DELETE with WHERE
        s.add("INSERT INTO orders (customer_id, created_at, amount, status) SELECT c.id, now(), 9.99, 'NEW' FROM customers c WHERE c.id < 5");
        s.add("UPDATE orders SET amount = amount * 1.01 WHERE status='PAID' AND created_at >= now() - interval '3 days'");
        s.add("DELETE FROM orders WHERE status='CANCELLED' AND created_at < now() - interval '90 days'");

        // CTE with DML (common pattern)
        s.add("WITH top_c AS (SELECT id FROM customers ORDER BY id DESC LIMIT 10) UPDATE orders SET status='SHIPPED' WHERE customer_id IN (SELECT id FROM top_c)");

        // RETURN list
        return s;
    }
}
