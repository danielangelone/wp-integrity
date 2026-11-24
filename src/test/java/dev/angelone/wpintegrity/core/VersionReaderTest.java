package dev.angelone.wpintegrity.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VersionReaderTest {

    @TempDir
    Path root;

    private final VersionReader reader = new VersionReader();

    private void versionPhp(String content) throws IOException {
        Files.createDirectories(root.resolve("wp-includes"));
        Files.writeString(root.resolve("wp-includes/version.php"), content);
    }

    @Test
    void readsSingleQuotedVersionAndLocale() throws IOException {
        versionPhp("<?php\n$wp_version = '6.5.3';\n$wp_local_package = 'pt_BR';\n");
        assertEquals(new WordPressVersion("6.5.3", "pt_BR"), reader.read(root, Optional.empty(), Optional.empty()));
    }

    @Test
    void readsDoubleQuotes() throws IOException {
        versionPhp("<?php\n$wp_version = \"6.4.1\";\n");
        assertEquals(new WordPressVersion("6.4.1", "en_US"), reader.read(root, Optional.empty(), Optional.empty()));
    }

    @Test
    void toleratesExtraSpacesAndTabs() throws IOException {
        versionPhp("<?php\n  $wp_version   =\t'6.5.3'  ;\n\t$wp_local_package='de_DE';\n");
        assertEquals(new WordPressVersion("6.5.3", "de_DE"), reader.read(root, Optional.empty(), Optional.empty()));
    }

    @Test
    void toleratesCrlfLineEndings() throws IOException {
        versionPhp("<?php\r\n$wp_version = '6.5.3';\r\n$wp_local_package = 'fr_FR';\r\n");
        assertEquals(new WordPressVersion("6.5.3", "fr_FR"), reader.read(root, Optional.empty(), Optional.empty()));
    }

    @Test
    void ignoresCommentedOutAssignment() throws IOException {
        versionPhp("<?php\n// $wp_version = '1.0';\n$wp_version = '6.5.3';\n");
        assertEquals("6.5.3", reader.read(root, Optional.empty(), Optional.empty()).version());
    }

    @Test
    void overridesWinOverTheFile() throws IOException {
        versionPhp("<?php\n$wp_version = '6.5.3';\n$wp_local_package = 'pt_BR';\n");
        assertEquals(new WordPressVersion("6.0", "es_ES"),
                reader.read(root, Optional.of("6.0"), Optional.of("es_ES")));
    }

    @Test
    void overrideWorksWithoutVersionFile() {
        assertEquals(new WordPressVersion("6.5.3", "en_US"),
                reader.read(root, Optional.of("6.5.3"), Optional.empty()));
    }

    @Test
    void failsWhenVersionCannotBeFound() {
        assertThrows(ScanException.class, () -> reader.read(root, Optional.empty(), Optional.empty()));
    }

    @Test
    void failsWhenFileHasNoVersion() throws IOException {
        versionPhp("<?php\n$something = 'else';\n");
        assertThrows(ScanException.class, () -> reader.read(root, Optional.empty(), Optional.empty()));
    }

    @Test
    void rejectsUnsafeCharactersBeforeTheyReachTheUrl() {
        assertThrows(ScanException.class, () -> reader.read(root, Optional.of("6.5&x=1"), Optional.empty()));
    }
}
