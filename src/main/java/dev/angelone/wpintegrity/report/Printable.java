package dev.angelone.wpintegrity.report;

/** File names and code snippets come from the scanned machine, so they are untrusted text. */
final class Printable {

    private Printable() {
    }

    /** Replaces control characters (newlines, escape sequences) so they cannot forge output lines. */
    static String plain(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        value.codePoints().forEach(cp -> sb.appendCodePoint(Character.isISOControl(cp) ? '?' : cp));
        return sb.toString();
    }
}
