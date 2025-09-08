package dev.paraplan.app;

public record PlanFeatures(
        double totalCost,
        long planRows,
        int depth,
        int seqScans,
        int indexScans,
        int hashJoins,
        int sortNodes,
        boolean parallelAware,
        boolean functionsInFilters,
        boolean likeLeadingWildcard
) {}
