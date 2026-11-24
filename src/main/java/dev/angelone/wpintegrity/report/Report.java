package dev.angelone.wpintegrity.report;

import dev.angelone.wpintegrity.core.ScanResult;

interface Report {

    String render(ScanResult result);
}
