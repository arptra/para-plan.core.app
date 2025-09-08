package dev.paraplan.app.model;

import java.util.Map;
public record PlanVariant(Map<String,String> toggles, double cost) {}
