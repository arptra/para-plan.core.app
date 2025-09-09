package dev.paraplan.app.model;

public record AnalyzeRequest(String connectionId, String schema, String sql, AnalyzeOptions options) {}
