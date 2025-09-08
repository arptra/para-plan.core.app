package dev.paraplan.app.service;

import dev.paraplan.app.model.LandscapeReport;
import dev.paraplan.app.model.PlanFeatures;
import dev.paraplan.app.model.PlanVariant;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

@Service
public class LandscapeService {
    private final DataSource ds;
    private final ExplainService explain;
    public LandscapeService(DataSource ds, ExplainService explain) { this.ds = ds; this.explain = explain; }

    public LandscapeReport scan(String sql) throws Exception {
        List<Map<String,String>> toggles = List.of(
                Map.of("enable_seqscan","off","enable_indexscan","on"),
                Map.of("enable_hashjoin","off","enable_nestloop","on"),
                Map.of("enable_mergejoin","on"),
                Map.of("enable_indexonlyscan","on"),
                Map.of("enable_bitmapscan","on")
        );
        String baseJson = explain.explainJson(sql);
        PlanFeatures baseF = explain.parse(baseJson, sql);
        double baseCost = baseF.totalCost();

        List<PlanVariant> variants = new ArrayList<>();
        double worst = baseCost;
        List<Double> vals = new ArrayList<>();
        vals.add(baseCost);

        try (Connection c = ds.getConnection()) {
            c.setAutoCommit(false);
            for (var t: toggles) {
                try (Statement st = c.createStatement()) {
                    for (var e: t.entrySet()) st.execute("SET LOCAL " + e.getKey() + " = " + e.getValue());
                    try (ResultSet rs = st.executeQuery("EXPLAIN (FORMAT JSON, COSTS) " + sql)) {
                        StringBuilder sb = new StringBuilder();
                        while (rs.next()) sb.append(rs.getString(1));
                        PlanFeatures f = explain.parse(sb.toString(), sql);
                        variants.add(new PlanVariant(t, f.totalCost()));
                        vals.add(f.totalCost());
                        if (f.totalCost() > worst) worst = f.totalCost();
                    }
                }
            }
            c.rollback();
        }

        double avg = vals.stream().mapToDouble(Double::doubleValue).average().orElse(baseCost);
        double var = vals.stream().mapToDouble(v -> (v-avg)*(v-avg)).sum() / vals.size();
        double regret = Math.max(0, worst - baseCost);
        double robustness = 1.0 / (1.0 + regret * (var==0?1:var));
        return new LandscapeReport(baseCost, worst, var, regret, robustness, variants);
    }
}
