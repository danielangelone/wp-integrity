package dev.angelone.wpintegrity.core;

import java.util.ArrayList;
import java.util.List;

/** UNKNOWN_IN_CORE_DIR: something in wp-admin/ or wp-includes/ that the official release does not ship. */
public final class UnknownInCoreDirCheck implements Check {

    @Override
    public List<Finding> run(ScanContext context) {
        return unknownEntries(context).stream()
                .map(e -> Finding.of(CheckId.UNKNOWN_IN_CORE_DIR, e.relativePath(),
                        "not listed in the official checksums"))
                .toList();
    }

    /** Also used to choose which files get the pattern search. */
    static List<FileEntry> unknownEntries(ScanContext context) {
        List<FileEntry> result = new ArrayList<>();
        for (FileEntry entry : context.tree().all()) {
            if (PathRules.isInCoreDir(entry.relativePath()) && !context.checksums().contains(entry.relativePath())) {
                result.add(entry);
            }
        }
        return result;
    }
}
