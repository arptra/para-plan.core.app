package dev.paraplan.app;

import java.util.List;
public record AnalyzeResponse(
        String fingerprint,
        String normalizedSql,
        PlanFeatures features,
        PredictedMetrics predicted,
        LandscapeReport landscape,
        SelectivityReport selectivity,
        Distribution monteCarlo,
        List<OutlierCase> outliers,
        List<Recommendation> recommendations
) {}
