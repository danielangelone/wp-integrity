package dev.angelone.wpintegrity.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CoreChecksTest {

    @TempDir
    Path root;

    private ScanContext context(TreeFixture fixture) {
        return new ScanContext(new TreeWalker().walk(root), fixture.checksums(), false);
    }

    @Test
    void cleanInstallHasNoFindings() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall();
        ScanContext ctx = context(fx);
        assertTrue(new CoreModifiedCheck().run(ctx).isEmpty());
        assertTrue(new CoreMissingCheck().run(ctx).isEmpty());
        assertTrue(new UnknownInCoreDirCheck().run(ctx).isEmpty());
    }

    @Test
    void reportsModifiedCoreFile() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall();
        fx.file("wp-login.php", "<?php\n// tampered\n");
        List<Finding> findings = new CoreModifiedCheck().run(context(fx));
        assertEquals(1, findings.size());
        assertEquals(CheckId.CORE_MODIFIED, findings.get(0).check());
        assertEquals("wp-login.php", findings.get(0).path());
        assertEquals(Severity.ERROR, findings.get(0).severity());
    }

    @Test
    void reportsMissingCoreFile() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().officialOnly("wp-cron.php", "<?php\n");
        List<Finding> findings = new CoreMissingCheck().run(context(fx));
        assertEquals(1, findings.size());
        assertEquals(CheckId.CORE_MISSING, findings.get(0).check());
        assertEquals("wp-cron.php", findings.get(0).path());
        assertEquals(Severity.WARNING, findings.get(0).severity());
    }

    @Test
    void reportsUnknownFileInCoreDirectories() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall();
        fx.file("wp-includes/class-evil.php", "<?php\n");
        fx.file("wp-admin/js/extra.js", "x");
        List<String> paths = new UnknownInCoreDirCheck().run(context(fx)).stream().map(Finding::path).toList();
        assertEquals(List.of("wp-admin/js/extra.js", "wp-includes/class-evil.php"), paths);
    }

    @Test
    void wpContentIsIgnoredEvenWhenTheChecksumsListIt() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall()
                .officialOnly("wp-content/themes/twentytwentythree/theme.json", "{}");
        fx.file("wp-content/plugins/mine/mine.php", "<?php\n");
        ScanContext ctx = context(fx);
        assertTrue(new CoreMissingCheck().run(ctx).isEmpty());
        assertTrue(new CoreModifiedCheck().run(ctx).isEmpty());
        assertTrue(new UnknownInCoreDirCheck().run(ctx).isEmpty());
    }

    @Test
    void handlesNamesWithSpacesAndAccents() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().core("wp-includes/ação especial.php", "<?php\n");
        fx.file("wp-includes/ação especial.php", "<?php\n// changed\n");
        List<Finding> findings = new CoreModifiedCheck().run(context(fx));
        assertEquals(1, findings.size());
        assertTrue(findings.get(0).path().startsWith("wp-includes/a"));
    }

    @Test
    void symlinkInPlaceOfCoreFileIsNotTrusted() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall();
        Path login = root.resolve("wp-login.php");
        Files.copy(login, root.resolve("real-copy.txt"));
        Files.delete(login);
        try {
            Files.createSymbolicLink(login, root.resolve("real-copy.txt"));
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "symbolic links not supported here");
        }
        List<Finding> findings = new CoreModifiedCheck().run(context(fx));
        assertEquals(1, findings.size());
        assertEquals("wp-login.php", findings.get(0).path());
    }
}
