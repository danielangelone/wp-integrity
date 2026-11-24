package dev.angelone.wpintegrity.core;

public record ScanContext(FileTree tree, Checksums checksums, boolean scanPlugins) {
}
