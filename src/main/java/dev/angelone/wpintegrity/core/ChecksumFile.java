package dev.angelone.wpintegrity.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads checksums saved from the API, for offline use. */
public final class ChecksumFile {

    public Checksums load(Path file, WordPressVersion version) {
        try {
            return Checksums.parse(Files.readString(file, StandardCharsets.UTF_8), version);
        } catch (IOException e) {
            throw new ScanException("Cannot read the checksums file " + file + ": " + e.getMessage(), e);
        }
    }
}
