package dev.angelone.wpintegrity.core;

import java.util.ArrayList;
import java.util.List;

/** EXTRA_PHP_IN_ROOT: a .php file next to wp-config.php that the official release does not ship. */
public final class ExtraPhpInRootCheck implements Check {

    private static final String CONFIG = "wp-config.php";

    @Override
    public List<Finding> run(ScanContext context) {
        return extraEntries(context).stream()
                .map(e -> Finding.of(CheckId.EXTRA_PHP_IN_ROOT, e.relativePath(), "not part of WordPress core"))
                .toList();
    }

    /** Also used to choose which files get the pattern search. */
    static List<FileEntry> extraEntries(ScanContext context) {
        List<FileEntry> result = new ArrayList<>();
        for (FileEntry entry : context.tree().all()) {
            String path = entry.relativePath();
            if (PathRules.isInRoot(path) && PathRules.isPhp(path) && !path.equals(CONFIG)
                    && !context.checksums().contains(path)) {
                result.add(entry);
            }
        }
        return result;
    }
}
