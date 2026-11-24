package dev.angelone.wpintegrity.core;

import java.util.Comparator;

/**
 * One sign worth a human look. {@code path} is relative to the scanned root and uses '/'.
 * {@code line}, {@code detail} and {@code snippet} are optional.
 */
public record Finding(CheckId check, String path, Integer line, String detail, String snippet) {

    public static final Comparator<Finding> REPORT_ORDER = Comparator
            .comparing((Finding f) -> f.severity()).reversed()
            .thenComparing(Finding::path)
            .thenComparing(f -> f.line() == null ? 0 : f.line())
            .thenComparing(f -> f.check().name());

    public static Finding of(CheckId check, String path, String detail) {
        return new Finding(check, path, null, detail, null);
    }

    public Severity severity() {
        return check.severity();
    }
}
