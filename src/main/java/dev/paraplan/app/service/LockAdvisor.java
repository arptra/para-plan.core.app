package dev.paraplan.app.service;

import dev.paraplan.app.model.LockReport;
import dev.paraplan.app.model.PredictedMetrics;
import dev.paraplan.app.util.SqlUtil;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LockAdvisor {
    public LockReport analyze(String sql, PredictedMetrics metrics) {
        if (sql == null) return new LockReport("UNKNOWN", List.of(), 0, List.of());
        String first = sql.trim().split("\\s+")[0].toUpperCase(Locale.ROOT);
        String level;
        switch (first) {
            case "UPDATE", "DELETE" -> level = "ROW EXCLUSIVE";
            case "INSERT" -> level = "ROW EXCLUSIVE";
            case "ALTER", "DROP", "TRUNCATE" -> level = "ACCESS EXCLUSIVE";
            default -> level = "ACCESS SHARE";
        }
        List<String> tables = SqlUtil.extractTableNames(sql);
        long estMs = metrics != null ? metrics.p95ms() : 0;
        List<String> advice = new ArrayList<>();
        if (!"ACCESS SHARE".equals(level) && estMs > 500) {
            advice.add("Рассмотрите запуск в off-hours");
            advice.add("Установите lock_timeout для безопасности");
        }
        return new LockReport(level, tables, estMs, advice);
    }
}
