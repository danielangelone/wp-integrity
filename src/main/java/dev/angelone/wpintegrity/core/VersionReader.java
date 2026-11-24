package dev.angelone.wpintegrity.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the version and language from wp-includes/version.php with plain regular expressions. */
public final class VersionReader {

    static final String DEFAULT_LOCALE = "en_US";
    private static final long MAX_BYTES = 64 * 1024;
    private static final Pattern VERSION = assignment("wp_version");
    private static final Pattern LOCALE = assignment("wp_local_package");
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]+");

    private static Pattern assignment(String variable) {
        return Pattern.compile("^[ \\t]*\\$" + variable + "[ \\t]*=[ \\t]*(['\"])([^'\"\\r\\n]*)\\1[ \\t]*;",
                Pattern.MULTILINE);
    }

    /**
     * Combines the overrides with what version.php says. An override always wins.
     *
     * @throws ScanException if no usable version can be found
     */
    public WordPressVersion read(Path root, Optional<String> versionOverride, Optional<String> localeOverride) {
        Path file = root.resolve("wp-includes").resolve("version.php");
        String content = readQuietly(file);
        String version = versionOverride.orElseGet(() -> extract(VERSION, content));
        if (version == null || version.isBlank()) {
            throw new ScanException("Cannot determine the WordPress version: wp-includes/version.php is missing, "
                    + "unreadable or has no $wp_version. Use --wp-version to set it.");
        }
        String locale = localeOverride.orElseGet(() -> extract(LOCALE, content));
        if (locale == null || locale.isBlank()) {
            locale = DEFAULT_LOCALE;
        }
        requireSafe("version", version);
        requireSafe("locale", locale);
        return new WordPressVersion(version, locale);
    }

    private static void requireSafe(String what, String value) {
        if (!SAFE.matcher(value).matches()) {
            throw new ScanException("Unexpected characters in the WordPress " + what + ": " + value);
        }
    }

    private static String extract(Pattern pattern, String content) {
        if (content == null) {
            return null;
        }
        Matcher m = pattern.matcher(content);
        return m.find() ? m.group(2).trim() : null;
    }

    private static String readQuietly(Path file) {
        try {
            if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || Files.size(file) > MAX_BYTES) {
                return null;
            }
            return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
