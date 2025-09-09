package dev.paraplan.app.model;

public record AnalyzeRequest(String sql, String connectionId, String schema, AnalyzeOptions options) {}
