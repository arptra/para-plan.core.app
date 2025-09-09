package dev.paraplan.app.service;

import dev.paraplan.app.model.PlanFeatures;
import dev.paraplan.app.model.PredictedMetrics;
import dev.paraplan.app.util.SqlUtil;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

@Service
public class CostPredictor {
    private final DataSource ds;
    public CostPredictor(DataSource ds) { this.ds = ds; }

    public PredictedMetrics predict(String sql, PlanFeatures f) {
        double base = 0.4 * f.totalCost();
        base += f.sortNodes() * 5;
        base += f.hashJoins() * 3;
        if (f.seqScans() > 0 && f.planRows() > 100_000) base *= 1.4;
        if (f.likeLeadingWildcard()) base *= 1.2;

        long estPages = 0;
        long estMem = 0;
        try (Connection c = ds.getConnection()) {
            List<String> tables = SqlUtil.extractTableNames(sql);
            for (String t : tables) {
                try (PreparedStatement ps = c.prepareStatement("SELECT relpages, reltuples FROM pg_class WHERE relname=?")) {
                    ps.setString(1, t);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            long relpages = rs.getLong(1);
                            long reltuples = (long)Math.max(1d, rs.getDouble(2));
                            long pages = (long)Math.round(f.planRows() * ((double)relpages / reltuples));
                            estPages += pages;
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("SELECT sum(avg_width) FROM pg_stats WHERE tablename=?")) {
                    ps.setString(1, t);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            long width = rs.getLong(1);
                            estMem += width * f.planRows();
                        }
                    }
                }
            }
        } catch (Exception ignored) { }

        int ioRisk = estPages > 1000 ? 80 : estPages > 200 ? 40 : 20;
        int tempRisk = estMem/1024/1024 > 100 ? 80 : estMem/1024/1024 > 10 ? 40 : 20;

        long p50 = Math.round(base + estPages * 0.1);
        long p95 = Math.round((base + estPages * 0.1) * 1.6);
        return new PredictedMetrics(p50, p95, tempRisk, ioRisk, estPages, estMem/1024);
    }
}
