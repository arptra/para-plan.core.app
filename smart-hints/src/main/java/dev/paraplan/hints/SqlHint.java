package dev.paraplan.hints;

/**
 * SQL hint with start and end positions and a suggestion.
 */
public record SqlHint(int start, int end, String message, String replacement) {}
