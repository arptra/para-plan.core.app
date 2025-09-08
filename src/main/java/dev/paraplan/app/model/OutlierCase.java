package dev.paraplan.app.model;

import java.util.Map;
public record OutlierCase(Map<String,Object> constants, long predictedMs) {}
