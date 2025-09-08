package dev.paraplan.app.model;

import java.util.List;
public record LandscapeReport(
        double baseCost,
        double worstCost,
        double variance,
        double regret,
        double robustnessScore,
        List<PlanVariant> variants
) {}
