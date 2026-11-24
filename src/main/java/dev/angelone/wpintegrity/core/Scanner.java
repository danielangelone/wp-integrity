package dev.angelone.wpintegrity.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Walks the installation once and runs every check over it. */
public final class Scanner {

    private final TreeWalker walker;
    private final List<Check> checks;

    public Scanner(TreeWalker walker, List<Check> checks) {
        this.walker = walker;
        this.checks = List.copyOf(checks);
    }

    public static Scanner standard() {
        return new Scanner(new TreeWalker(), List.of(
                new CoreModifiedCheck(),
                new CoreMissingCheck(),
                new UnknownInCoreDirCheck(),
                new PhpInUploadsCheck(),
                new ExtraPhpInRootCheck(),
                new WorldWritableCheck(),
                new SuspiciousPatternCheck()));
    }

    public ScanResult scan(Path root, WordPressVersion version, Checksums checksums, boolean scanPlugins,
                           String toolVersion) {
        FileTree tree = walker.walk(root);
        ScanContext context = new ScanContext(tree, checksums, scanPlugins);
        List<Finding> findings = new ArrayList<>(tree.unreadable());
        for (Check check : checks) {
            findings.addAll(check.run(context));
        }
        return new ScanResult(toolVersion, version, findings);
    }
}
