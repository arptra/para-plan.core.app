package dev.paraplan.app.model;

import java.util.List;

public record AnalyzeResponse(
    PlanFeatures features,
    PredictedMetrics predicted,
    LandscapeReport landscape,
    SelectivityReport selectivity,
    Distribution distribution,
    LockReport locks,
    ServerFit serverFit,
    List<String> nPlusOne,
    List<Recommendation> recommendations,
    List<String> advice
) {}
