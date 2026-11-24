package dev.angelone.wpintegrity.report;

import dev.angelone.wpintegrity.core.ScanResult;

public enum ReportFormat {
    TEXT(new TextReport()),
    JSON(new JsonReport()),
    HTML(new HtmlReport());

    private final Report report;

    ReportFormat(Report report) {
        this.report = report;
    }

    public String render(ScanResult result) {
        return report.render(result);
    }
}
