package dev.angelone.wpintegrity.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SuspiciousPatternCheckTest {

    @TempDir
    Path root;

    private List<Finding> run(TreeFixture fx, boolean scanPlugins) {
        return new SuspiciousPatternCheck().run(
                new ScanContext(new TreeWalker().walk(root), fx.checksums(), scanPlugins));
    }

    private Finding single(String code) throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().file("wp-content/uploads/t.php", "<?php\n" + code + "\n");
        List<Finding> findings = run(fx, false);
        assertEquals(1, findings.size(), code);
        return findings.get(0);
    }

    @Test
    void evalOfDecodedStrings() throws IOException {
        for (String fn : List.of("base64_decode", "gzinflate", "gzuncompress", "str_rot13")) {
            Finding f = single("eval(" + fn + "($x));");
            assertEquals(CheckId.SUSPICIOUS_PATTERN, f.check());
            assertEquals(2, f.line());
            assertEquals(Signature.EVAL_OF_DECODED.label(), f.detail());
            root.resolve("wp-content/uploads/t.php").toFile().delete();
        }
    }

    @Test
    void evalOfNestedDecodersAndSpacing() throws IOException {
        assertEquals(Signature.EVAL_OF_DECODED.label(), single("EVAL ( @ gzinflate(base64_decode($a)) );").detail());
    }

    @Test
    void commandWithRequestInput() throws IOException {
        for (String fn : List.of("system", "exec", "shell_exec", "passthru", "popen", "proc_open")) {
            assertEquals(Signature.COMMAND_FROM_REQUEST.label(), single(fn + "($_GET['c']);").detail());
            root.resolve("wp-content/uploads/t.php").toFile().delete();
        }
        assertEquals(Signature.COMMAND_FROM_REQUEST.label(), single("system('ls ' . $_POST['d']);").detail());
    }

    @Test
    void assertWithRequestInput() throws IOException {
        assertEquals(Signature.ASSERT_FROM_REQUEST.label(), single("assert($_REQUEST['x']);").detail());
    }

    @Test
    void pregReplaceWithEModifier() throws IOException {
        assertEquals(Signature.PREG_REPLACE_EVAL.label(), single("preg_replace('/x/e', $_GET['r'], $s);").detail());
        assertEquals(Signature.PREG_REPLACE_EVAL.label(), single("preg_replace(\"#x#ie\", 'y', $s);").detail());
    }

    @Test
    void longBase64Literal() throws IOException {
        assertEquals(Signature.LONG_BASE64_LITERAL.label(), single("base64_decode('" + "QUJD".repeat(80) + "');").detail());
    }

    @Test
    void ordinaryCodeIsNotFlagged() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().file("wp-content/uploads/ok.php", String.join("\n",
                "<?php",
                "$data = base64_decode('aGVsbG8=');",
                "$r = preg_replace('/x/i', 'y', $s);",
                "$r = preg_replace('/x/', 'ye', $s);",
                "system('uptime');",
                "echo $_GET['name'];",
                "eval($code);",
                "assert($value > 0);"));
        assertTrue(run(fx, false).isEmpty());
    }

    @Test
    void snippetIsTruncatedToOneHundredTwentyCharacters() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().file("wp-content/uploads/t.php",
                "<?php\neval(base64_decode('" + "A".repeat(500) + "'));\n");
        List<Finding> findings = run(fx, false);
        assertEquals(2, findings.size(), "one finding per matching pattern");
        Finding f = findings.get(0);
        assertEquals(120, f.snippet().length());
        assertTrue(f.snippet().endsWith("..."));
        assertTrue(f.snippet().startsWith("eval(base64_decode("));
    }

    @Test
    void crlfLinesAreCountedAndTrimmed() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall()
                .file("wp-content/uploads/t.php", "<?php\r\n\r\neval(gzinflate($x));\r\n");
        Finding f = run(fx, false).get(0);
        assertEquals(3, f.line());
        assertEquals("eval(gzinflate($x));", f.snippet());
    }

    @Test
    void searchesOnlyTheRightPlaces() throws IOException {
        String bad = "<?php eval(base64_decode($x));";
        TreeFixture fx = new TreeFixture(root).cleanInstall()
                .file("wp-content/uploads/a.php", bad)
                .file("wp-content/uploads/a.txt", bad)
                .file("wp-includes/unknown.php", bad)
                .file("shell.php", bad)
                .file("wp-config.php", bad)
                .file("wp-content/plugins/p/p.php", bad)
                .file("wp-content/themes/t/functions.php", bad);
        assertEquals(List.of("shell.php", "wp-content/uploads/a.php", "wp-includes/unknown.php"),
                run(fx, false).stream().map(Finding::path).sorted().toList());
        assertEquals(5, run(fx, true).size());
    }

    @Test
    void coreFilesThatMatchTheOfficialChecksumAreNotSearched() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().core("wp-includes/legit.php", "<?php eval(base64_decode($x));");
        assertTrue(run(fx, false).isEmpty());
    }

    @Test
    void filesAboveTwoMebibytesAreSkipped() throws IOException {
        TreeFixture fx = new TreeFixture(root).cleanInstall().file("wp-content/uploads/big.php", "<?php");
        try (RandomAccessFile f = new RandomAccessFile(root.resolve("wp-content/uploads/big.php").toFile(), "rw")) {
            f.setLength(2L * 1024 * 1024 + 1);
        }
        List<Finding> findings = run(fx, false);
        assertEquals(1, findings.size());
        assertEquals(CheckId.SKIPPED, findings.get(0).check());
        assertEquals("wp-content/uploads/big.php", findings.get(0).path());
    }

    @Test
    void unreadableFilesAreSkipped() throws IOException {
        assumeTrue(root.getFileSystem().supportedFileAttributeViews().contains("posix"));
        assumeFalse("root".equals(System.getProperty("user.name")), "root ignores permissions");
        TreeFixture fx = new TreeFixture(root).cleanInstall().file("wp-content/uploads/locked.php", "<?php");
        Path locked = root.resolve("wp-content/uploads/locked.php");
        Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("---------"));
        try {
            List<Finding> findings = run(fx, false);
            assertEquals(1, findings.size());
            assertEquals(CheckId.SKIPPED, findings.get(0).check());
        } finally {
            Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("rw-------"));
        }
    }
}
