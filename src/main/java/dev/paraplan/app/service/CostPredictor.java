package dev.paraplan.app.service;

import dev.paraplan.app.model.PlanFeatures;
import dev.paraplan.app.model.PredictedMetrics;
import org.springframework.stereotype.Service;

@Service
public class CostPredictor {
    public PredictedMetrics predict(PlanFeatures f) {
        double base = 0.4 * f.totalCost();
        base += f.sortNodes() * 5;
        base += f.hashJoins() * 3;
        if (f.seqScans() > 0 && f.planRows() > 100_000) base *= 1.4;
        if (f.likeLeadingWildcard()) base *= 1.2;

        int tempRisk = f.sortNodes() > 0 && f.planRows() > 50_000 ? 70 : 20;
        int ioRisk = f.seqScans() > 0 && f.planRows() > 200_000 ? 60 : 20;
        long p50 = Math.round(base);
        long p95 = Math.round(base * 1.6);
        return new PredictedMetrics(p50, p95, tempRisk, ioRisk);
    }
}
