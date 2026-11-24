package dev.angelone.wpintegrity.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** Builds a small fake WordPress tree in a temporary directory, with matching official checksums. */
public final class TreeFixture {

    public static final WordPressVersion VERSION = new WordPressVersion("6.5.3", "en_US");

    private final Path root;
    private final Map<String, String> official = new TreeMap<>();

    public TreeFixture(Path root) {
        this.root = root;
    }

    public Path root() {
        return root;
    }

    /** A core file that exists on disk and is listed in the official checksums with the same content. */
    public TreeFixture core(String path, String content) throws IOException {
        file(path, content);
        official.put(path, md5(content.getBytes(StandardCharsets.UTF_8)));
        return this;
    }

    /** A core file listed in the official checksums but absent from disk. */
    public TreeFixture officialOnly(String path, String content) {
        official.put(path, md5(content.getBytes(StandardCharsets.UTF_8)));
        return this;
    }

    public TreeFixture file(String path, String content) throws IOException {
        Path target = root.resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return this;
    }

    /** The smallest tree the checks need: version.php plus a few real-looking core files. */
    public TreeFixture cleanInstall() throws IOException {
        core("wp-includes/version.php", "<?php\n$wp_version = '6.5.3';\n");
        core("index.php", "<?php\nrequire __DIR__ . '/wp-blog-header.php';\n");
        core("wp-login.php", "<?php\n// login\n");
        core("wp-admin/index.php", "<?php\n// admin\n");
        core("wp-includes/functions.php", "<?php\n// functions\n");
        file("wp-config.php", "<?php\n// config\n");
        file("wp-content/index.php", "<?php\n// Silence is golden.\n");
        return this;
    }

    public Checksums checksums() {
        return new Checksums(official);
    }

    public String checksumsJson() {
        StringBuilder sb = new StringBuilder("{\"checksums\":{");
        boolean first = true;
        for (Map.Entry<String, String> e : official.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
        }
        return sb.append("}}").toString();
    }

    static String md5(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
