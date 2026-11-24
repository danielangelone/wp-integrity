package dev.angelone.wpintegrity.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dev.angelone.wpintegrity.core.Finding;
import dev.angelone.wpintegrity.core.ScanResult;
import dev.angelone.wpintegrity.core.Severity;
import java.util.List;

final class JsonReport implements Report {

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    record Counts(long errors, long warnings, long info) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Item(String id, String severity, String description, String path, Integer line, String detail,
                String snippet) {
    }

    record Document(String toolVersion, String wordpressVersion, String locale, Counts counts, List<Item> findings) {
    }

    @Override
    public String render(ScanResult result) {
        List<Item> items = result.findings().stream().map(JsonReport::item).toList();
        Counts counts = new Counts(result.count(Severity.ERROR), result.count(Severity.WARNING),
                result.count(Severity.INFO));
        Document doc = new Document(result.toolVersion(), result.wordpress().version(), result.wordpress().locale(),
                counts, items);
        try {
            return mapper.writeValueAsString(doc) + "\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize the report", e);
        }
    }

    private static Item item(Finding f) {
        return new Item(f.check().name(), f.severity().name(), f.check().description(), f.path(), f.line(),
                f.detail(), f.snippet());
    }
}
