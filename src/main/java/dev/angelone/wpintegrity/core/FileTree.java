package dev.angelone.wpintegrity.core;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Everything found under the root, keyed by relative path, plus the places that could not be listed. */
public record FileTree(Path root, Map<String, FileEntry> entries, List<Finding> unreadable) {

    public FileTree {
        entries = Collections.unmodifiableMap(entries);
        unreadable = List.copyOf(unreadable);
    }

    public boolean has(String relativePath) {
        return entries.containsKey(relativePath);
    }

    public Collection<FileEntry> all() {
        return entries.values();
    }
}
