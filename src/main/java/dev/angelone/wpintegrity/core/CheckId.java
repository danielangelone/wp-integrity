package dev.angelone.wpintegrity.core;

import static dev.angelone.wpintegrity.core.Severity.ERROR;
import static dev.angelone.wpintegrity.core.Severity.INFO;
import static dev.angelone.wpintegrity.core.Severity.WARNING;

public enum CheckId {
    CORE_MODIFIED(ERROR, "Core file differs from the official checksum"),
    PHP_IN_UPLOADS(ERROR, "PHP file inside wp-content/uploads"),
    CORE_MISSING(WARNING, "Core file is missing"),
    UNKNOWN_IN_CORE_DIR(WARNING, "File in wp-admin/ or wp-includes/ is not in the official checksums"),
    EXTRA_PHP_IN_ROOT(WARNING, "PHP file in the root that is not part of core"),
    WORLD_WRITABLE(WARNING, "PHP file writable by any user"),
    SUSPICIOUS_PATTERN(INFO, "Code pattern that often indicates obfuscation or remote execution"),
    SKIPPED(INFO, "File could not be read or is too large, skipped");

    private final Severity severity;
    private final String description;

    CheckId(Severity severity, String description) {
        this.severity = severity;
        this.description = description;
    }

    public Severity severity() {
        return severity;
    }

    public String description() {
        return description;
    }
}
