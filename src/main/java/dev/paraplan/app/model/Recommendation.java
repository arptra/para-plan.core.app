package dev.paraplan.app.model;

public record Recommendation(String kind, String title, String reason, String example, int impactScore, String effort) {}
