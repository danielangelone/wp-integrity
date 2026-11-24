package dev.angelone.wpintegrity.core;

import java.nio.file.Path;

/** A path found under the scanned root. Symbolic links are listed but never followed. */
public record FileEntry(String relativePath, Path absolutePath, boolean symlink, boolean regularFile, long size) {
}
