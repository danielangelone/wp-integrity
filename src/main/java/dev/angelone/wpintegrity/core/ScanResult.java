package dev.angelone.wpintegrity.core;

import java.util.List;

public record ScanResult(String toolVersion, WordPressVersion wordpress, List<Finding> findings) {

    public ScanResult {
        findings = findings.stream().sorted(Finding.REPORT_ORDER).toList();
    }

    public long count(Severity severity) {
        return findings.stream().filter(f -> f.severity() == severity).count();
    }

    /** True when at least one finding is at or above the threshold. */
    public boolean hasFindingsAtOrAbove(Severity threshold) {
        return findings.stream().anyMatch(f -> f.severity().compareTo(threshold) >= 0);
    }
}
