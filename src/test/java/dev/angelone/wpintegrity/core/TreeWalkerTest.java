package dev.angelone.wpintegrity.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.Normalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TreeWalkerTest {

    @TempDir
    Path root;

    @Test
    void listsFilesWithRelativeSlashPaths() throws IOException {
        new TreeFixture(root).file("wp-admin/css/a.css", "x").file("index.php", "<?php");
        FileTree tree = new TreeWalker().walk(root);
        assertTrue(tree.has("wp-admin/css/a.css"));
        assertTrue(tree.has("index.php"));
        assertEquals(2, tree.entries().size());
    }

    @Test
    void handlesSpacesAndAccentsInNames() throws IOException {
        new TreeFixture(root).file("wp-content/uploads/relatório final 2024.php", "x")
                .file("wp-content/uploads/açaí/ção.txt", "y");
        FileTree tree = new TreeWalker().walk(root);
        // some file systems store names decomposed, so compare in a normalized form
        assertTrue(tree.entries().keySet().stream()
                .map(k -> Normalizer.normalize(k, Normalizer.Form.NFC))
                .anyMatch("wp-content/uploads/relatório final 2024.php"::equals));
        assertEquals(2, tree.entries().size());
    }

    @Test
    void doesNotFollowSymlinkLoops() throws IOException {
        new TreeFixture(root).file("wp-content/uploads/real.txt", "x");
        Path loop = root.resolve("wp-content/uploads/loop");
        try {
            Files.createSymbolicLink(loop, root.resolve("wp-content"));
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "symbolic links not supported here");
        }
        FileTree tree = new TreeWalker().walk(root);
        FileEntry link = tree.entries().get("wp-content/uploads/loop");
        assertTrue(link.symlink());
        assertFalse(link.regularFile());
        assertFalse(tree.has("wp-content/uploads/loop/uploads/real.txt"));
        assertEquals(2, tree.entries().size());
    }

    @Test
    void doesNotDescendIntoLinksPointingOutsideTheRoot(@TempDir Path outside) throws IOException {
        Files.writeString(outside.resolve("secret.txt"), "x");
        try {
            Files.createSymbolicLink(root.resolve("out"), outside);
        } catch (UnsupportedOperationException | IOException e) {
            assumeTrue(false, "symbolic links not supported here");
        }
        FileTree tree = new TreeWalker().walk(root);
        assertEquals(1, tree.entries().size());
        assertTrue(tree.entries().get("out").symlink());
    }

    @Test
    void recordsUnreadableDirectories() throws IOException {
        assumeTrue(root.getFileSystem().supportedFileAttributeViews().contains("posix"));
        assumeFalse("root".equals(System.getProperty("user.name")), "root ignores permissions");
        Path locked = root.resolve("locked");
        Files.createDirectories(locked);
        Files.writeString(locked.resolve("a.php"), "x");
        Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("---------"));
        try {
            FileTree tree = new TreeWalker().walk(root);
            assertEquals(1, tree.unreadable().size());
            assertEquals(CheckId.SKIPPED, tree.unreadable().get(0).check());
            assertEquals("locked", tree.unreadable().get(0).path());
        } finally {
            Files.setPosixFilePermissions(locked, PosixFilePermissions.fromString("rwx------"));
        }
    }
}
