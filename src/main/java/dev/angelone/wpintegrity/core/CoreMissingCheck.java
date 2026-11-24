package dev.angelone.wpintegrity.core;

import java.util.ArrayList;
import java.util.List;

/** CORE_MISSING: a file the official release has but the installation does not. */
public final class CoreMissingCheck implements Check {

    @Override
    public List<Finding> run(ScanContext context) {
        List<Finding> findings = new ArrayList<>();
        for (String path : context.checksums().paths()) {
            if (!PathRules.isInWpContent(path) && !context.tree().has(path)) {
                findings.add(Finding.of(CheckId.CORE_MISSING, path, "listed in the official checksums but not found"));
            }
        }
        return findings;
    }
}
