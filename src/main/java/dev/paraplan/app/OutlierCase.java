package dev.paraplan.app;

import java.util.Map;
public record OutlierCase(Map<String,Object> constants, long predictedMs) {}
