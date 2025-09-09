package dev.paraplan.app.model;

public record PredictedMetrics(long p50ms,
                               long p95ms,
                               int tempSpillRisk,
                               int ioRisk,
                               long estimatedPages,
                               long estimatedMemKB) {}
