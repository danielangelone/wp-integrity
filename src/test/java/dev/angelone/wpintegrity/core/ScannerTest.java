package dev.angelone.wpintegrity.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScannerTest {

    @TempDir
    Path root;

    private ScanResult scan(TreeFixture fx) {
        return Scanner.standard().scan(root, TreeFixture.VERSION, fx.checksums(), false, "test");
    }

    @Test
    void cleanInstallHasNoFindings() throws IOException {
        ScanResult result = scan(new TreeFixture(root).cleanInstall());
        assertTrue(result.findings().isEmpty());
        assertFalse(result.hasFindingsAtOrAbove(Severity.INFO));
    }

    @Test
    void everyCheckIdCanBeProducedAndIsOrderedBySeverityThenPath() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().officialOnly("wp-cron.php", "<?php\n");
        fx.file("wp-login.php", "<?php\n// tampered\n");
        fx.file("wp-content/uploads/x.php", "<?php eval(base64_decode($a));\n");
        fx.file("wp-includes/extra.php", "<?php\n");
        fx.file("backdoor.php", "<?php\n");
        List<CheckId> ids = scan(fx).findings().stream().map(Finding::check).toList();
        assertEquals(List.of(CheckId.PHP_IN_UPLOADS, CheckId.CORE_MODIFIED, CheckId.EXTRA_PHP_IN_ROOT,
                CheckId.CORE_MISSING, CheckId.UNKNOWN_IN_CORE_DIR, CheckId.SUSPICIOUS_PATTERN), ids);
    }

    @Test
    void thresholdsCountSeverities() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().file("backdoor.php", "<?php\n");
        ScanResult result = scan(fx);
        assertEquals(1, result.count(Severity.WARNING));
        assertTrue(result.hasFindingsAtOrAbove(Severity.WARNING));
        assertFalse(result.hasFindingsAtOrAbove(Severity.ERROR));
    }
}
