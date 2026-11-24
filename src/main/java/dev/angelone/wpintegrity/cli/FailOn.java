package dev.angelone.wpintegrity.cli;

import dev.angelone.wpintegrity.core.ScanResult;
import dev.angelone.wpintegrity.core.Severity;

enum FailOn {
    NONE(null),
    WARNING(Severity.WARNING),
    ERROR(Severity.ERROR);

    private final Severity threshold;

    FailOn(Severity threshold) {
        this.threshold = threshold;
    }

    boolean tripsOn(ScanResult result) {
        return threshold != null && result.hasFindingsAtOrAbove(threshold);
    }
}
