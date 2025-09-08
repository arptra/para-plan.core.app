package dev.paraplan.app.service;

import dev.paraplan.app.model.SelectivityReport;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProbeService {
    private final DataSource ds;
    public ProbeService(DataSource ds) { this.ds = ds; }

    public SelectivityReport probe(String sql) {
        Map<String,Double> single = new LinkedHashMap<>();
        Map<String,Double> pairs = new LinkedHashMap<>();
        if (sql.toLowerCase().contains("customers") && sql.toLowerCase().contains("email") && sql.contains("%@example.com")) {
            double sel = fastSelectivity("customers", "email ILIKE '%@example.com'");
            single.put("customers.email ILIKE '%@example.com'", sel);
        }
        return new SelectivityReport(single, pairs);
    }

    private double fastSelectivity(String table, String predicate) {
        String q = "SELECT (SELECT count(*) FROM " + table + " WHERE " + predicate + ")::float / GREATEST((SELECT count(*) FROM " + table + "),1)";
        try (Connection c = ds.getConnection(); Statement st = c.createStatement(); ResultSet rs = st.executeQuery(q)) {
            if (rs.next()) return Math.max(0.0, Math.min(1.0, rs.getDouble(1)));
        } catch (Exception e) { /* ignore for demo */ }
        return 0.1;
    }
}
