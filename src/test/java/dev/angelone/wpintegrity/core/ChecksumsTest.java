package dev.angelone.wpintegrity.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ChecksumsTest {

    private static final WordPressVersion V = new WordPressVersion("6.5.3", "en_US");
    private static final String SMALL = "{\"checksums\":{\"index.php\":\"926dd0f95df723f9ed934eb058882cc8\"}}";

    @TempDir
    Path tmp;

    @Test
    void parsesTheRealApiResponse() throws IOException {
        // Captured from api.wordpress.org on 2026-11-24: version=6.5.3&locale=en_US
        String json = new String(getClass().getResourceAsStream("/real-checksums-6.5.3-en_US.json").readAllBytes(),
                StandardCharsets.UTF_8);
        Checksums checksums = Checksums.parse(json, V);
        assertEquals(2998, checksums.paths().size());
        assertEquals("926dd0f95df723f9ed934eb058882cc8", checksums.md5ByPath().get("index.php"));
        assertTrue(checksums.contains("wp-includes/version.php"));
    }

    @Test
    void falseChecksumsMeansUnknownVersion() {
        ScanException e = assertThrows(ScanException.class, () -> Checksums.parse("{\"checksums\":false}", V));
        assertTrue(e.getMessage().contains("6.5.3"));
    }

    @Test
    void rejectsGarbage() {
        assertThrows(ScanException.class, () -> Checksums.parse("<html>", V));
        assertThrows(ScanException.class, () -> Checksums.parse("{}", V));
        assertThrows(ScanException.class, () -> Checksums.parse("{\"checksums\":{}}", V));
        assertThrows(ScanException.class, () -> Checksums.parse("{\"checksums\":{\"a.php\":\"nothex\"}}", V));
    }

    @Test
    void clientSendsVersionLocaleAndUserAgent() throws Exception {
        try (StubApi api = new StubApi(200, SMALL)) {
            Checksums checksums = new ChecksumClient(api.uri(), "9.9.9").fetch(new WordPressVersion("6.5.3", "pt_BR"));
            assertTrue(checksums.contains("index.php"));
            assertEquals("version=6.5.3&locale=pt_BR", api.lastQuery());
            assertEquals("wp-integrity/9.9.9", api.lastUserAgent());
        }
    }

    @Test
    void clientFailsOnUnknownVersion() throws Exception {
        try (StubApi api = new StubApi(200, "{\"checksums\":false}")) {
            assertThrows(ScanException.class, () -> new ChecksumClient(api.uri(), "t").fetch(V));
        }
    }

    @Test
    void clientFailsOnServerError() throws Exception {
        try (StubApi api = new StubApi(503, "")) {
            ScanException e = assertThrows(ScanException.class, () -> new ChecksumClient(api.uri(), "t").fetch(V));
            assertTrue(e.getMessage().contains("503"));
        }
    }

    @Test
    void clientFailsWhenNothingListens() throws Exception {
        URI dead;
        try (StubApi api = new StubApi(200, SMALL)) {
            dead = api.uri();
        }
        assertThrows(ScanException.class, () -> new ChecksumClient(dead, "t").fetch(V));
    }

    @Test
    void readsChecksumsFromLocalFile() throws IOException {
        Path file = tmp.resolve("sums.json");
        Files.writeString(file, SMALL);
        assertFalse(new ChecksumFile().load(file, V).paths().isEmpty());
        assertThrows(ScanException.class, () -> new ChecksumFile().load(tmp.resolve("missing.json"), V));
    }
}
