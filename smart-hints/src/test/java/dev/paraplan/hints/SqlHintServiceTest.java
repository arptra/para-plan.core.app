package dev.paraplan.hints;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SqlHintServiceTest {

    @Test
    void detectsSimpleIssues() {
        SqlHintService service = new SqlHintService();
        String sql = "SELECT * FROM users WHERE name LIKE '%foo%'";
        List<SqlHint> hints = service.analyze(sql, List.of());
        assertEquals(2, hints.size());
        assertTrue(hints.stream().anyMatch(h -> h.message().contains("SELECT *")));
        assertTrue(hints.stream().anyMatch(h -> h.message().contains("Leading wildcard")));
    }

    @Test
    void highlightsRecommendationFragments() {
        SqlHintService service = new SqlHintService();
        String sql = "SELECT o.id FROM orders o WHERE o.created_at > now()";
        RecommendationDto rec = new RecommendationDto("INDEX", "Добавить индекс по условию", "CREATE INDEX idx ON orders(created_at);");
        List<SqlHint> hints = service.analyze(sql, List.of(rec));
        assertTrue(hints.stream().anyMatch(h -> h.message().contains("Добавить индекс")));
    }

    @Test
    void highlightsStatsRecommendation() {
        SqlHintService service = new SqlHintService();
        String sql = "SELECT c.name FROM customers c WHERE c.region_id = 42";
        RecommendationDto rec = new RecommendationDto("STATS", "Обновить статистику для customers", "ANALYZE customers;");
        List<SqlHint> hints = service.analyze(sql, List.of(rec));
        assertTrue(hints.stream().anyMatch(h -> h.message().contains("статистику")));
    }

    @Test
    void coversConfigRecommendation() {
        SqlHintService service = new SqlHintService();
        String sql = "SELECT * FROM big_table ORDER BY name";
        RecommendationDto rec = new RecommendationDto("CONFIG", "Увеличьте work_mem для крупных сортировок", "SET work_mem='128MB';");
        List<SqlHint> hints = service.analyze(sql, List.of(rec));
        assertTrue(hints.stream().anyMatch(h -> h.start() == 0 && h.end() == sql.length()));
    }

    @Test
    void highlightsRewriteRecommendation() {
        SqlHintService service = new SqlHintService();
        String sql = "SELECT * FROM users WHERE name LIKE '%foo%'";
        RecommendationDto rec = new RecommendationDto("REWRITE", "Перепишите LIKE на 'foo%'", "LIKE '%foo%'");
        List<SqlHint> hints = service.analyze(sql, List.of(rec));
        assertTrue(hints.stream().anyMatch(h -> h.message().contains("Перепишите LIKE")));
    }
}
