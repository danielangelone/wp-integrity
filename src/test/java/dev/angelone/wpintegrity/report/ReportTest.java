package dev.angelone.wpintegrity.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.angelone.wpintegrity.core.CheckId;
import dev.angelone.wpintegrity.core.Finding;
import dev.angelone.wpintegrity.core.ScanResult;
import dev.angelone.wpintegrity.core.WordPressVersion;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportTest {

    private static final WordPressVersion V = new WordPressVersion("6.5.3", "en_US");

    private static ScanResult sample() {
        return new ScanResult("0.1.0", V, List.of(
                new Finding(CheckId.SUSPICIOUS_PATTERN, "wp-content/uploads/a.php", 7, "eval applied to a decoded string",
                        "eval(base64_decode($x));"),
                Finding.of(CheckId.CORE_MISSING, "wp-cron.php", "listed in the official checksums but not found"),
                Finding.of(CheckId.CORE_MODIFIED, "wp-login.php", "md5 differs from the official checksum"),
                Finding.of(CheckId.CORE_MODIFIED, "index.php", "md5 differs from the official checksum")));
    }

    @Test
    void textIsOrderedBySeverityThenPathAndEndsWithSummary() {
        String[] lines = ReportFormat.TEXT.render(sample()).split("\n");
        assertTrue(lines[0].startsWith("ERROR") && lines[0].contains("index.php"));
        assertTrue(lines[1].startsWith("ERROR") && lines[1].contains("wp-login.php"));
        assertTrue(lines[2].startsWith("WARNING") && lines[2].contains("wp-cron.php"));
        assertTrue(lines[3].startsWith("INFO") && lines[3].contains("wp-content/uploads/a.php:7"));
        assertTrue(lines[4].contains("eval(base64_decode($x));"));
        assertEquals("WordPress 6.5.3 (en_US): 2 errors, 1 warnings, 1 info", lines[lines.length - 1]);
    }

    @Test
    void textOfACleanScanIsJustTheSummary() {
        assertEquals("WordPress 6.5.3 (en_US): 0 errors, 0 warnings, 0 info\n",
                ReportFormat.TEXT.render(new ScanResult("0.1.0", V, List.of())));
    }

    @Test
    void textCannotBeUsedToForgeLines() {
        ScanResult result = new ScanResult("0.1.0", V,
                List.of(Finding.of(CheckId.PHP_IN_UPLOADS, "a.php\nERROR FAKE\u001b[2J", "x")));
        String text = ReportFormat.TEXT.render(result);
        assertFalse(text.contains("\u001b"));
        assertEquals(2, text.split("\n").length);
    }

    @Test
    void jsonHasVersionsCountsAndFindings() throws IOException {
        JsonNode json = new ObjectMapper().readTree(ReportFormat.JSON.render(sample()));
        assertEquals("0.1.0", json.get("toolVersion").asText());
        assertEquals("6.5.3", json.get("wordpressVersion").asText());
        assertEquals("en_US", json.get("locale").asText());
        assertEquals(2, json.get("counts").get("errors").asInt());
        assertEquals(1, json.get("counts").get("warnings").asInt());
        assertEquals(1, json.get("counts").get("info").asInt());
        JsonNode first = json.get("findings").get(0);
        assertEquals("CORE_MODIFIED", first.get("id").asText());
        assertEquals("ERROR", first.get("severity").asText());
        assertEquals("index.php", first.get("path").asText());
        assertFalse(first.has("line"));
        JsonNode pattern = json.get("findings").get(3);
        assertEquals(7, pattern.get("line").asInt());
        assertEquals("eval(base64_decode($x));", pattern.get("snippet").asText());
    }

    @Test
    void htmlEscapesFileNamesAndSnippets() {
        ScanResult result = new ScanResult("0.1.0", V, List.of(
                new Finding(CheckId.SUSPICIOUS_PATTERN, "wp-content/uploads/<script>alert(1)</script>.php", 1,
                        "d\"<b>", "<img src=x onerror=alert(1)>")));
        String html = ReportFormat.HTML.render(result);
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("<img"));
        assertFalse(html.contains("<b>"));
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;.php"));
        assertTrue(html.contains("&lt;img src=x onerror=alert(1)&gt;"));
    }

    @Test
    void htmlLoadsNothingExternalAndRunsNoScript() {
        String html = ReportFormat.HTML.render(sample());
        assertFalse(html.contains("<script"));
        assertFalse(html.contains("<link"));
        assertFalse(html.contains("src="));
        assertFalse(html.contains("http://"));
        assertFalse(html.contains("https://"));
        assertTrue(html.contains("default-src 'none'"));
    }
}
