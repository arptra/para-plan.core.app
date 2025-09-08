package dev.paraplan.app.model;

import java.util.Map;
public record SelectivityReport(Map<String,Double> single, Map<String,Double> pairs) {}
