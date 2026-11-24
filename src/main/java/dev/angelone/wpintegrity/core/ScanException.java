package dev.angelone.wpintegrity.core;

/** A problem that stops the scan; the command line maps it to exit code 2. */
public class ScanException extends RuntimeException {

    public ScanException(String message) {
        super(message);
    }

    public ScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
