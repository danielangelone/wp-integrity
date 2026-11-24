package dev.angelone.wpintegrity.report;

import dev.angelone.wpintegrity.core.Finding;
import dev.angelone.wpintegrity.core.ScanResult;
import dev.angelone.wpintegrity.core.Severity;

final class TextReport implements Report {

    @Override
    public String render(ScanResult result) {
        StringBuilder sb = new StringBuilder();
        for (Finding f : result.findings()) {
            sb.append(String.format("%-7s %-19s %s", f.severity(), f.check(), Printable.plain(f.path())));
            if (f.line() != null) {
                sb.append(':').append(f.line());
            }
            if (f.detail() != null) {
                sb.append(" - ").append(Printable.plain(f.detail()));
            }
            sb.append('\n');
            if (f.snippet() != null) {
                sb.append("        ").append(Printable.plain(f.snippet())).append('\n');
            }
        }
        sb.append(String.format("WordPress %s (%s): %d errors, %d warnings, %d info%n",
                result.wordpress().version(), result.wordpress().locale(),
                result.count(Severity.ERROR), result.count(Severity.WARNING), result.count(Severity.INFO)));
        return sb.toString();
    }
}
