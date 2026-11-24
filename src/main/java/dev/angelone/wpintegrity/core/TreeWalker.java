package dev.angelone.wpintegrity.core;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Lists the tree under a root without following symbolic links: a link to a directory is recorded as
 * a single entry and never descended into, so loops and links pointing outside the root are harmless.
 */
public final class TreeWalker {

    public FileTree walk(Path root) {
        Map<String, FileEntry> entries = new TreeMap<>();
        List<Finding> unreadable = new ArrayList<>();
        try {
            Files.walkFileTree(root, EnumSet.noneOf(java.nio.file.FileVisitOption.class), Integer.MAX_VALUE,
                    new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                            String relative = relative(root, file);
                            entries.put(relative, new FileEntry(relative, file, attrs.isSymbolicLink(),
                                    attrs.isRegularFile(), attrs.size()));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException e) {
                            unreadable.add(Finding.of(CheckId.SKIPPED, relative(root, file),
                                    "could not be listed: " + e.getClass().getSimpleName()));
                            return FileVisitResult.CONTINUE;
                        }
                    });
        } catch (IOException e) {
            throw new ScanException("Cannot scan " + root + ": " + e.getMessage(), e);
        }
        return new FileTree(root, entries, unreadable);
    }

    private static String relative(Path root, Path file) {
        StringBuilder sb = new StringBuilder();
        for (Path part : root.relativize(file)) {
            if (sb.length() > 0) {
                sb.append('/');
            }
            sb.append(part);
        }
        return sb.toString();
    }
}
