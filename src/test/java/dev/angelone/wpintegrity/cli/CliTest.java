package dev.angelone.wpintegrity.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.angelone.wpintegrity.core.StubApi;
import dev.angelone.wpintegrity.core.TreeFixture;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliTest {

    private static final URI UNUSED = URI.create("http://127.0.0.1:9/");

    @TempDir
    Path base;

    private Path site;
    private Path sums;

    private final ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errBytes = new ByteArrayOutputStream();

    private TreeFixture install() throws IOException {
        site = Files.createDirectories(base.resolve("site"));
        TreeFixture fx = new TreeFixture(site).cleanInstall();
        sums = base.resolve("sums.json");
        return fx;
    }

    private int run(URI api, String... args) {
        return new WpIntegrityCommand(api, new PrintStream(outBytes, true, StandardCharsets.UTF_8),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8)).commandLine().execute(args);
    }

    private int offline(TreeFixture fx, String... extra) throws IOException {
        Files.writeString(sums, fx.checksumsJson());
        String[] args = Stream.concat(Stream.of(site.toString(), "--checksums", sums.toString()), Stream.of(extra))
                .toArray(String[]::new);
        return run(UNUSED, args);
    }

    private String out() {
        return outBytes.toString(StandardCharsets.UTF_8);
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void cleanInstallExitsZero() throws IOException {
        assertEquals(0, offline(install()));
        assertTrue(out().contains("0 errors, 0 warnings, 0 info"));
    }

    @Test
    void failOnMatrixForAnError() throws IOException {
        TreeFixture fx = install();
        fx.file("wp-content/uploads/x.php", "<?php");
        assertEquals(1, offline(fx));
        assertEquals(0, offline(fx, "--fail-on", "none"));
        assertEquals(1, offline(fx, "--fail-on", "warning"));
        assertEquals(1, offline(fx, "--fail-on", "error"));
    }

    @Test
    void failOnMatrixForAWarning() throws IOException {
        TreeFixture fx = install();
        fx.file("extra.php", "<?php");
        assertEquals(0, offline(fx));
        assertEquals(0, offline(fx, "--fail-on", "none"));
        assertEquals(1, offline(fx, "--fail-on", "warning"));
        assertEquals(0, offline(fx, "--fail-on", "error"));
    }

    @Test
    void infoAloneNeverFails() throws IOException {
        TreeFixture fx = install();
        fx.file("wp-content/plugins/p/p.php", "<?php eval(base64_decode($x));");
        assertEquals(0, offline(fx, "--scan-plugins", "--fail-on", "warning"));
        assertTrue(out().contains("SUSPICIOUS_PATTERN"));
    }

    @Test
    void pluginsAreOnlySearchedOnRequest() throws IOException {
        TreeFixture fx = install();
        fx.file("wp-content/plugins/p/p.php", "<?php eval(base64_decode($x));");
        assertEquals(0, offline(fx));
        assertFalse(out().contains("SUSPICIOUS_PATTERN"));
    }

    @Test
    void jsonGoesToStdoutOrToAFile() throws IOException {
        TreeFixture fx = install();
        assertEquals(0, offline(fx, "--format", "json"));
        assertTrue(out().contains("\"wordpressVersion\" : \"6.5.3\""));
        Path file = base.resolve("report.json");
        assertEquals(0, offline(fx, "--format", "JSON", "--output", file.toString()));
        assertTrue(Files.readString(file).contains("\"findings\""));
    }

    @Test
    void htmlNeedsAnOutputFile() throws IOException {
        TreeFixture fx = install();
        assertEquals(2, offline(fx, "--format", "html"));
        assertTrue(err().contains("--output"));
        Path file = base.resolve("report.html");
        assertEquals(0, offline(fx, "--format", "html", "--output", file.toString()));
        assertTrue(Files.readString(file).startsWith("<!DOCTYPE html>"));
    }

    @Test
    void refusesOutputInsideTheScannedFolder() throws IOException {
        TreeFixture fx = install();
        Path inside = site.resolve("wp-content/report.json");
        assertEquals(2, offline(fx, "--format", "json", "--output", inside.toString()));
        assertFalse(Files.exists(inside));
        assertTrue(err().contains("outside the scanned folder"));
        Path viaDots = site.resolve("wp-content/../report.txt");
        assertEquals(2, offline(fx, "--output", viaDots.toString()));
        assertFalse(Files.exists(site.resolve("report.txt")));
    }

    @Test
    void refusesOutputThatIsALinkIntoTheScannedFolder() throws IOException {
        TreeFixture fx = install();
        Path real = site.resolve("target.json");
        Files.writeString(real, "keep");
        Path link = base.resolve("link.json");
        try {
            Files.createSymbolicLink(link, real);
        } catch (UnsupportedOperationException | IOException e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "symbolic links not supported here");
        }
        assertEquals(2, offline(fx, "--format", "json", "--output", link.toString()));
        assertEquals("keep", Files.readString(real));
    }

    @Test
    void scanLeavesTheTreeUntouched() throws IOException {
        TreeFixture fx = install();
        fx.file("wp-content/uploads/x.php", "<?php");
        List<String> before = listing();
        offline(fx, "--format", "html", "--output", base.resolve("r.html").toString());
        assertEquals(before, listing());
    }

    private List<String> listing() throws IOException {
        try (Stream<Path> s = Files.walk(site)) {
            return s.map(p -> p + ":" + p.toFile().length() + ":" + p.toFile().lastModified()).sorted().toList();
        }
    }

    @Test
    void usageErrorsExitTwo() {
        assertEquals(2, run(UNUSED));
        assertEquals(2, run(UNUSED, base.resolve("nope").toString()));
        assertEquals(2, run(UNUSED, base.toString(), "--format", "pdf"));
        assertEquals(2, run(UNUSED, base.toString(), "--fail-on", "info"));
    }

    @Test
    void missingVersionFileNeedsAnOverride() throws IOException {
        TreeFixture fx = install();
        Files.delete(site.resolve("wp-includes/version.php"));
        assertEquals(2, offline(fx));
        assertTrue(err().contains("--wp-version"));
        assertEquals(0, offline(fx, "--wp-version", "6.5.3", "--locale", "en_US"));
        assertTrue(out().contains("CORE_MISSING"));
    }

    @Test
    void apiAnswerIsUsedWhenNoLocalFileIsGiven() throws Exception {
        TreeFixture fx = install();
        try (StubApi api = new StubApi(200, fx.checksumsJson())) {
            assertEquals(0, run(api.uri(), site.toString()));
            assertEquals("version=6.5.3&locale=en_US", api.lastQuery());
        }
    }

    @Test
    void unknownVersionExitsTwoInsteadOfLookingClean() throws Exception {
        install();
        try (StubApi api = new StubApi(200, "{\"checksums\":false}")) {
            assertEquals(2, run(api.uri(), site.toString(), "--fail-on", "none"));
            assertTrue(err().contains("no checksums"));
            assertEquals("", out());
        }
    }

    @Test
    void apiFailureExitsTwoInsteadOfLookingClean() throws Exception {
        install();
        try (StubApi api = new StubApi(500, "")) {
            assertEquals(2, run(api.uri(), site.toString(), "--fail-on", "none"));
            assertTrue(err().contains("500"));
        }
        assertEquals(2, run(UNUSED, site.toString()));
        assertEquals("", out());
    }

    @Test
    void helpAndVersionExitZero() {
        assertEquals(0, run(UNUSED, "--help"));
        assertTrue(out().contains("--scan-plugins"));
        assertEquals(0, run(UNUSED, "--version"));
        assertTrue(out().contains("wp-integrity"));
    }
}
