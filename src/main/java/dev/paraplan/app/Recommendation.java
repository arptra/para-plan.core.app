package dev.paraplan.app;

public record Recommendation(String kind, String title, String reason, String example, int impactScore, String effort) {}
