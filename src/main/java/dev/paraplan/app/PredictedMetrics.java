package dev.paraplan.app;

public record PredictedMetrics(long p50ms, long p95ms, int tempSpillRisk, int ioRisk) {}
