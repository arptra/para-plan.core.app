
package dev.paraplan.app;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdvisorService {
    public List<Recommendation> advise(String sql, PlanFeatures f, PredictedMetrics pm) {
        List<Recommendation> recs = new ArrayList<>();
        if (f.seqScans() > 0 && f.indexScans()==0 && f.planRows() > 100_000) {
            recs.add(new Recommendation("INDEX",
                    "Seq Scan на большой таблице",
                    "Создайте селективный индекс или сузьте фильтры",
                    "-- пример: CREATE INDEX CONCURRENTLY idx_tbl_col ON table(col);",
                    70, "MEDIUM"));
        }
        if (f.sortNodes() > 0 && f.planRows() > 50_000) {
            recs.add(new Recommendation("PARAM",
                    "Риск temp на сортировке/хэше",
                    "Увеличьте work_mem локально или создайте покрывающий индекс под ORDER BY",
                    "SET LOCAL work_mem='128MB';",
                    40, "LOW"));
        }
        if (f.functionsInFilters()) {
            recs.add(new Recommendation("SQL",
                    "Функции в предикатах",
                    "Функция на колонке мешает использованию индекса",
                    "-- пример: CREATE INDEX CONCURRENTLY idx_fun ON table((lower(col)));",
                    30,"MEDIUM"));
        }
        if (f.likeLeadingWildcard()) {
            recs.add(new Recommendation("INDEX",
                    "LIKE с ведущим %",
                    "Рассмотрите триграммный индекс (при разрешении) либо альтернативную схему поиска",
                    "-- пример: CREATE EXTENSION IF NOT EXISTS pg_trgm; CREATE INDEX CONCURRENTLY idx_trgm ON customers USING gin (email gin_trgm_ops);",
                    60,"HIGH"));
        }
        return recs;
    }
}
