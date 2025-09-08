package dev.paraplan.app;

import java.util.Map;
public record SelectivityReport(Map<String,Double> single, Map<String,Double> pairs) {}
