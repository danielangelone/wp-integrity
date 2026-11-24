package dev.angelone.wpintegrity.core;

import java.util.Locale;
import java.util.regex.Pattern;

/** Pure functions that classify relative paths. */
public final class PathRules {

    private static final Pattern PHP_EXTENSION = Pattern.compile("\\.(php|phtml|phar|pht|php[0-9])$");
    private static final String UPLOADS = "wp-content/uploads/";

    private PathRules() {
    }

    public static boolean isPhp(String path) {
        return PHP_EXTENSION.matcher(path.toLowerCase(Locale.ROOT)).find();
    }

    public static boolean isInUploads(String path) {
        return path.startsWith(UPLOADS);
    }

    public static boolean isInCoreDir(String path) {
        return path.startsWith("wp-admin/") || path.startsWith("wp-includes/");
    }

    public static boolean isInRoot(String path) {
        return path.indexOf('/') < 0;
    }

    /** wp-content is not part of the official core, except for the uploads checks. */
    public static boolean isInWpContent(String path) {
        return path.startsWith("wp-content/");
    }

    public static boolean isPluginOrTheme(String path) {
        return path.startsWith("wp-content/plugins/") || path.startsWith("wp-content/themes/");
    }
}
