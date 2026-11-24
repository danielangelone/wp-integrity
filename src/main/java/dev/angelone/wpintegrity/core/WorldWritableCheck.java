package dev.angelone.wpintegrity.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;

/** WORLD_WRITABLE: a PHP file any user on the machine can change. Only meaningful on POSIX file systems. */
public final class WorldWritableCheck implements Check {

    @Override
    public List<Finding> run(ScanContext context) {
        List<Finding> findings = new ArrayList<>();
        for (FileEntry entry : context.tree().all()) {
            if (!entry.regularFile() || !PathRules.isPhp(entry.relativePath())) {
                continue;
            }
            try {
                PosixFileAttributes attrs = Files.readAttributes(entry.absolutePath(), PosixFileAttributes.class,
                        LinkOption.NOFOLLOW_LINKS);
                if (attrs.permissions().contains(PosixFilePermission.OTHERS_WRITE)) {
                    findings.add(Finding.of(CheckId.WORLD_WRITABLE, entry.relativePath(), "mode allows writes by anyone"));
                }
            } catch (UnsupportedOperationException e) {
                return List.of();
            } catch (IOException e) {
                // the attributes could not be read; the file is covered by the other checks
            }
        }
        return findings;
    }
}
