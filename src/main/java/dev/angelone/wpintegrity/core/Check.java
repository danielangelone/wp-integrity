package dev.angelone.wpintegrity.core;

import java.util.List;

/** One verification. Checks only read; they never modify the scanned tree. */
public interface Check {

    List<Finding> run(ScanContext context);
}
