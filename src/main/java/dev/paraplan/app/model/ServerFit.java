package dev.paraplan.app.model;

public record ServerFit(
        String workMem,
        String sharedBuffers,
        String effectiveCacheSize
) {}
