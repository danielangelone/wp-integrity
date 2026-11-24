package dev.angelone.wpintegrity.core;

import java.util.ArrayList;
import java.util.List;

/** PHP_IN_UPLOADS: executable PHP has no business in the uploads directory. */
public final class PhpInUploadsCheck implements Check {

    @Override
    public List<Finding> run(ScanContext context) {
        return phpEntries(context).stream()
                .map(e -> Finding.of(CheckId.PHP_IN_UPLOADS, e.relativePath(), "PHP file in the uploads directory"))
                .toList();
    }

    /** Also used to choose which files get the pattern search. */
    static List<FileEntry> phpEntries(ScanContext context) {
        List<FileEntry> result = new ArrayList<>();
        for (FileEntry entry : context.tree().all()) {
            if (PathRules.isInUploads(entry.relativePath()) && PathRules.isPhp(entry.relativePath())) {
                result.add(entry);
            }
        }
        return result;
    }
}
