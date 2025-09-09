package dev.paraplan.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.paraplan.app.model.PlanFeatures;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Locale;

@Service
public class ExplainService {
    private final ConnectionManager connections;
    private static final ObjectMapper M = new ObjectMapper();
    public ExplainService(ConnectionManager connections) { this.connections = connections; }

    public String explainJson(String connectionId, String schema, String sql) throws Exception {
        try (Connection c = connections.getConnection(connectionId, schema);
             PreparedStatement ps = c.prepareStatement("EXPLAIN (FORMAT JSON, COSTS, BUFFERS) " + sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                StringBuilder sb = new StringBuilder();
                while (rs.next()) sb.append(rs.getString(1));
                return sb.toString();
            }
        }
    }

    public PlanFeatures parse(String explainJson, String originalSql) throws Exception {
        JsonNode root = M.readTree(explainJson);
        JsonNode plan = root.get(0).get("Plan");
        double totalCost = plan.has("Total Cost") ? plan.get("Total Cost").asDouble(0d) : 0d;
        long planRows = plan.has("Plan Rows") ? plan.get("Plan Rows").asLong(0) : 0;
        Walker w = new Walker(); w.walk(plan);
        boolean likeLeading = containsLeadingWildcardLike(originalSql);
        return new PlanFeatures(totalCost, planRows, w.maxDepth, w.seqScans, w.indexScans, w.hashJoins, w.sortNodes, w.parallel, w.funcInFilters, likeLeading);
    }

    private static boolean containsLeadingWildcardLike(String sql) {
        String s = sql.toLowerCase(Locale.ROOT);
        return s.contains(" like '%") || s.contains(" ilike '%");
    }

    static class Walker {
        int maxDepth=0, seqScans=0, indexScans=0, hashJoins=0, sortNodes=0; boolean parallel=false, funcInFilters=false;
        void walk(JsonNode n) { walk(n,1); }
        void walk(JsonNode n, int d) {
            maxDepth = Math.max(maxDepth, d);
            String t = n.has("Node Type") ? n.get("Node Type").asText() : null;
            if ("Seq Scan".equals(t)) seqScans++;
            if (t!=null && t.contains("Index Scan")) indexScans++;
            if (t!=null && t.contains("Hash Join")) hashJoins++;
            if (t!=null && (t.contains("Sort") || n.has("Sort Key"))) sortNodes++;
            if (n.has("Parallel Aware") && n.get("Parallel Aware").asBoolean(false)) parallel = true;
            if (n.has("Filter") && n.get("Filter").asText().matches(".*\\w+\\(.*")) funcInFilters = true;
            if (n.has("Plans")) for (JsonNode ch : n.get("Plans")) walk(ch, d+1);
        }
    }
}
