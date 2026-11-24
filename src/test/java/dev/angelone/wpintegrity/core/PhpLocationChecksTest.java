package dev.angelone.wpintegrity.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PhpLocationChecksTest {

    @TempDir
    Path root;

    private ScanContext context(TreeFixture fixture) {
        return new ScanContext(new TreeWalker().walk(root), fixture.checksums(), false);
    }

    @Test
    void flagsEveryPhpExtensionInUploads() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall();
        for (String name : List.of("a.php", "b.phtml", "c.phar", "d.pht", "e.php5", "f.PHP", "g.php7", "h.php0")) {
            fx.file("wp-content/uploads/2024/" + name, "x");
        }
        List<Finding> findings = new PhpInUploadsCheck().run(context(fx));
        assertEquals(8, findings.size());
        assertTrue(findings.stream().allMatch(f -> f.check() == CheckId.PHP_IN_UPLOADS));
        assertEquals(Severity.ERROR, findings.get(0).severity());
    }

    @Test
    void ignoresHarmlessUploadsAndPhpElsewhere() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall();
        fx.file("wp-content/uploads/photo.jpg", "x");
        fx.file("wp-content/uploads/notes.php.txt", "x");
        fx.file("wp-content/uploads/readme.phps", "x");
        fx.file("wp-content/plugins/p/p.php", "x");
        assertTrue(new PhpInUploadsCheck().run(context(fx)).isEmpty());
    }

    @Test
    void phpInUploadsWithSpacesAndAccents() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().file("wp-content/uploads/meu arquivo ção.php", "x");
        assertEquals(1, new PhpInUploadsCheck().run(context(fx)).size());
    }

    @Test
    void flagsExtraPhpInRootButNotConfigOrCore() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall();
        fx.file("shell.php", "x");
        fx.file("backup.PHP", "x");
        fx.file("notes.txt", "x");
        List<Finding> findings = new ExtraPhpInRootCheck().run(context(fx));
        assertEquals(List.of("backup.PHP", "shell.php"), findings.stream().map(Finding::path).toList());
        assertEquals(Severity.WARNING, findings.get(0).severity());
    }

    @Test
    void flagsWorldWritablePhpOnly() throws IOException {
        assumeTrue(root.getFileSystem().supportedFileAttributeViews().contains("posix"));
        TreeFixture fx = new TreeFixture(root).cleanInstall();
        fx.file("open.php", "x");
        fx.file("open.txt", "x");
        fx.file("closed.php", "x");
        Files.setPosixFilePermissions(root.resolve("open.php"), PosixFilePermissions.fromString("rw-rw-rw-"));
        Files.setPosixFilePermissions(root.resolve("open.txt"), PosixFilePermissions.fromString("rw-rw-rw-"));
        Files.setPosixFilePermissions(root.resolve("closed.php"), PosixFilePermissions.fromString("rw-r--r--"));
        List<Finding> findings = new WorldWritableCheck().run(context(fx));
        assertEquals(1, findings.size());
        assertEquals("open.php", findings.get(0).path());
        assertEquals(CheckId.WORLD_WRITABLE, findings.get(0).check());
    }
}
