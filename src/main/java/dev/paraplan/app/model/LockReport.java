package dev.paraplan.app.model;

import java.util.List;

public record LockReport(
        String level,
        List<String> objects,
        long estimatedMs,
        List<String> advice
) {}
