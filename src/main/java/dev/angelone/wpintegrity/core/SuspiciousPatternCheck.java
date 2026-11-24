package dev.angelone.wpintegrity.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SUSPICIOUS_PATTERN and SKIPPED. Only files that are already odd (PHP in uploads, unknown files in
 * the core directories, extra PHP in the root) are searched, plus plugins and themes on request.
 */
public final class SuspiciousPatternCheck implements Check {

    static final long MAX_BYTES = 2L * 1024 * 1024;
    static final int SNIPPET_LENGTH = 120;

    @Override
    public List<Finding> run(ScanContext context) {
        List<Finding> findings = new ArrayList<>();
        for (FileEntry entry : targets(context).values()) {
            if (entry.size() > MAX_BYTES) {
                findings.add(Finding.of(CheckId.SKIPPED, entry.relativePath(), "larger than 2 MiB"));
                continue;
            }
            String content;
            try {
                content = new String(Files.readAllBytes(entry.absolutePath()), StandardCharsets.UTF_8);
            } catch (IOException e) {
                findings.add(Finding.of(CheckId.SKIPPED, entry.relativePath(), "could not be read"));
                continue;
            }
            scan(entry.relativePath(), content, findings);
        }
        return findings;
    }

    private static Map<String, FileEntry> targets(ScanContext context) {
        Map<String, FileEntry> targets = new LinkedHashMap<>();
        List<FileEntry> candidates = new ArrayList<>();
        candidates.addAll(PhpInUploadsCheck.phpEntries(context));
        candidates.addAll(UnknownInCoreDirCheck.unknownEntries(context));
        candidates.addAll(ExtraPhpInRootCheck.extraEntries(context));
        if (context.scanPlugins()) {
            context.tree().all().stream()
                    .filter(e -> PathRules.isPluginOrTheme(e.relativePath()) && PathRules.isPhp(e.relativePath()))
                    .forEach(candidates::add);
        }
        for (FileEntry entry : candidates) {
            if (entry.regularFile()) {
                targets.putIfAbsent(entry.relativePath(), entry);
            }
        }
        return targets;
    }

    private static void scan(String path, String content, List<Finding> findings) {
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].endsWith("\r") ? lines[i].substring(0, lines[i].length() - 1) : lines[i];
            for (Signature signature : Signature.all()) {
                int at = signature.find(line);
                if (at >= 0) {
                    findings.add(new Finding(CheckId.SUSPICIOUS_PATTERN, path, i + 1, signature.label(),
                            snippet(line, at)));
                }
            }
        }
    }

    static String snippet(String line, int from) {
        String text = line.substring(from).strip();
        if (text.length() <= SNIPPET_LENGTH) {
            return text;
        }
        return text.substring(0, SNIPPET_LENGTH - 3) + "...";
    }
}
