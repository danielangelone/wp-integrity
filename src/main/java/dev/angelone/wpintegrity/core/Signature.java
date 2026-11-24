package dev.angelone.wpintegrity.core;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The patterns searched for, one line at a time. They are deliberately few and plain: each one is
 * a heuristic that matches code that is rarely written by hand, and each has false positives.
 */
public enum Signature {

    /** eval(base64_decode(...)) and the gzinflate, gzuncompress and str_rot13 variants. */
    EVAL_OF_DECODED("eval applied to a decoded string",
            "\\beval\\s*\\(\\s*@?\\s*(?:base64_decode|gzinflate|gzuncompress|str_rot13)\\s*\\("),

    /** system(), exec() and friends called with request input on the same line. */
    COMMAND_FROM_REQUEST("command execution with request input",
            "\\b(?:system|exec|shell_exec|passthru|popen|proc_open)\\s*\\([^;\\r\\n]*\\$_(?:GET|POST|REQUEST|COOKIE)\\b"),

    /** assert() evaluates a string as code on old PHP versions. */
    ASSERT_FROM_REQUEST("assert with request input",
            "\\bassert\\s*\\([^;\\r\\n]*\\$_(?:GET|POST|REQUEST|COOKIE)\\b"),

    /** The /e modifier makes preg_replace evaluate the replacement as code (removed in PHP 7). */
    PREG_REPLACE_EVAL("preg_replace with the /e modifier",
            "\\bpreg_replace\\s*\\(\\s*(['\"])(.)(?:(?!\\2).)*\\2[a-zA-Z]*e[a-zA-Z]*\\1"),

    /** A literal of 256 or more base64 characters handed straight to base64_decode. */
    LONG_BASE64_LITERAL("long base64 literal passed to base64_decode",
            "\\bbase64_decode\\s*\\(\\s*(['\"])[A-Za-z0-9+/=]{256,}\\1");

    private final String label;
    private final Pattern pattern;

    Signature(String label, String regex) {
        this.label = label;
        this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public String label() {
        return label;
    }

    /** Index of the first match in the line, or -1. */
    public int find(String line) {
        var matcher = pattern.matcher(line);
        return matcher.find() ? matcher.start() : -1;
    }

    public static List<Signature> all() {
        return List.of(values());
    }
}
