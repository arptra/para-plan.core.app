package dev.paraplan.app;

import java.util.Map;
public record PlanVariant(Map<String,String> toggles, double cost) {}
