package dev.paraplan.app.service;

import dev.paraplan.app.model.Distribution;
import dev.paraplan.app.model.PlanFeatures;
import dev.paraplan.app.model.PredictedMetrics;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MonteCarloService {
    private final CostPredictor predictor;
    private final ExplainService explain;
    public MonteCarloService(CostPredictor predictor, ExplainService explain) {
        this.predictor = predictor; this.explain = explain;
    }

    public Distribution simulate(String connectionId, String schema, String sql, int samples) throws Exception {
        samples = Math.max(10, Math.min(200, samples));
        Random rnd = new Random(42);
        List<Long> lat = new ArrayList<>();
        for (int i=0;i<samples;i++) {
            String json = explain.explainJson(connectionId, schema, sql);
            PlanFeatures f = explain.parse(json, sql);
            PredictedMetrics pm = predictor.predict(f);
            long jitter = (long)(pm.p50ms() * (0.8 + rnd.nextDouble()*0.8));
            lat.add(pm.p50ms() + jitter/10);
        }
        lat.sort(Long::compareTo);
        long p50 = lat.get((int)(0.50*lat.size()));
        long p95 = lat.get((int)(0.95*lat.size())-1);
        long p99 = lat.get((int)(0.99*lat.size())-1);
        return new Distribution(p50,p95,p99);
    }
}
