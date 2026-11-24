package dev.angelone.wpintegrity.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Official MD5 checksums keyed by path relative to the WordPress root.
 * <p>
 * The api.wordpress.org response is {@code {"checksums": {"path": "md5", ...}}} and MD5 is the only
 * algorithm it offers. For a version or locale it does not know, it answers 200 with
 * {@code {"checksums": false}}.
 */
public record Checksums(Map<String, String> md5ByPath) {

    private static final Pattern MD5 = Pattern.compile("[0-9a-f]{32}");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public Checksums {
        md5ByPath = Collections.unmodifiableMap(new TreeMap<>(md5ByPath));
    }

    public boolean contains(String path) {
        return md5ByPath.containsKey(path);
    }

    public Set<String> paths() {
        return md5ByPath.keySet();
    }

    /** Parses the API response body. */
    public static Checksums parse(String json, WordPressVersion version) {
        JsonNode root;
        try {
            root = MAPPER.readTree(json);
        } catch (IOException e) {
            throw new ScanException("The checksums response is not valid JSON", e);
        }
        JsonNode node = root == null ? null : root.get("checksums");
        if (node == null || !node.isObject()) {
            throw new ScanException("WordPress.org has no checksums for version " + version.version()
                    + " (locale " + version.locale() + "). Check --wp-version and --locale.");
        }
        Map<String, String> result = new TreeMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            String hash = entry.getValue().isTextual() ? entry.getValue().asText().toLowerCase(Locale.ROOT) : "";
            if (!MD5.matcher(hash).matches()) {
                throw new ScanException("The checksums response has an unexpected value for " + entry.getKey());
            }
            result.put(entry.getKey(), hash);
        }
        if (result.isEmpty()) {
            throw new ScanException("The checksums response is empty");
        }
        return new Checksums(result);
    }
}
