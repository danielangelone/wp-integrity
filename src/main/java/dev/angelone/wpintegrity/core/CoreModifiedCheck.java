package dev.angelone.wpintegrity.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** CORE_MODIFIED: a core file whose hash differs from the official one. */
public final class CoreModifiedCheck implements Check {

    @Override
    public List<Finding> run(ScanContext context) {
        List<Finding> findings = new ArrayList<>();
        for (Map.Entry<String, String> expected : context.checksums().md5ByPath().entrySet()) {
            String path = expected.getKey();
            if (PathRules.isInWpContent(path)) {
                continue;
            }
            FileEntry entry = context.tree().entries().get(path);
            if (entry == null) {
                continue;
            }
            if (!entry.regularFile()) {
                findings.add(Finding.of(CheckId.CORE_MODIFIED, path,
                        entry.symlink() ? "is a symbolic link, not the official file" : "is not a regular file"));
                continue;
            }
            try {
                if (!Md5.of(entry.absolutePath()).equals(expected.getValue())) {
                    findings.add(Finding.of(CheckId.CORE_MODIFIED, path, "md5 differs from the official checksum"));
                }
            } catch (IOException e) {
                findings.add(Finding.of(CheckId.SKIPPED, path, "could not be read to compute its hash"));
            }
        }
        return findings;
    }
}
