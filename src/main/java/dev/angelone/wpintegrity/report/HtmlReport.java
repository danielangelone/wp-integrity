package dev.angelone.wpintegrity.report;

import dev.angelone.wpintegrity.core.Finding;
import dev.angelone.wpintegrity.core.ScanResult;
import dev.angelone.wpintegrity.core.Severity;

/**
 * One self-contained file: styles are inline, nothing is loaded from elsewhere and there is no
 * script. Every value that came from the scanned machine goes through {@link #escape}.
 */
final class HtmlReport implements Report {

    private static final String STYLE = """
            body{font:14px/1.5 system-ui,sans-serif;margin:2rem auto;max-width:70rem;padding:0 1rem;color:#1a1a1a}
            h1{font-size:1.4rem;margin-bottom:.2rem}
            .meta{color:#555;margin-bottom:1.5rem}
            .counts span{display:inline-block;margin-right:1rem;padding:.2rem .6rem;border-radius:4px;background:#eee}
            table{border-collapse:collapse;width:100%;margin-top:1rem}
            th,td{text-align:left;vertical-align:top;padding:.4rem .6rem;border-bottom:1px solid #ddd}
            th{background:#f5f5f5}
            code{font:12px ui-monospace,monospace;word-break:break-all}
            .ERROR{color:#a40000;font-weight:600}
            .WARNING{color:#8a5a00;font-weight:600}
            .INFO{color:#2b5d8a}
            .empty{margin-top:1rem;padding:1rem;background:#f0f7f0;border-radius:4px}
            @media print{body{margin:0;max-width:none}th{background:none}tr{break-inside:avoid}}
            """;

    @Override
    public String render(ScanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
                .append("<meta http-equiv=\"Content-Security-Policy\" content=\"default-src 'none'; style-src 'unsafe-inline'\">\n")
                .append("<title>wp-integrity report</title>\n<style>").append(STYLE).append("</style>\n</head>\n<body>\n");
        sb.append("<h1>wp-integrity report</h1>\n<div class=\"meta\">WordPress ")
                .append(escape(result.wordpress().version())).append(" (")
                .append(escape(result.wordpress().locale())).append(") &middot; wp-integrity ")
                .append(escape(result.toolVersion())).append("</div>\n");
        sb.append("<div class=\"counts\"><span>").append(result.count(Severity.ERROR)).append(" errors</span><span>")
                .append(result.count(Severity.WARNING)).append(" warnings</span><span>")
                .append(result.count(Severity.INFO)).append(" info</span></div>\n");
        if (result.findings().isEmpty()) {
            sb.append("<p class=\"empty\">No findings.</p>\n");
        } else {
            sb.append("<table>\n<tr><th>Severity</th><th>Check</th><th>File</th><th>Line</th><th>Detail</th></tr>\n");
            for (Finding f : result.findings()) {
                row(sb, f);
            }
            sb.append("</table>\n");
        }
        sb.append("<p class=\"meta\">Indications for a person to review. This is not an antivirus.</p>\n");
        return sb.append("</body>\n</html>\n").toString();
    }

    private static void row(StringBuilder sb, Finding f) {
        sb.append("<tr><td class=\"").append(f.severity()).append("\">").append(f.severity()).append("</td><td>")
                .append(f.check()).append("</td><td><code>").append(escape(f.path())).append("</code></td><td>")
                .append(f.line() == null ? "" : f.line().toString()).append("</td><td>");
        if (f.detail() != null) {
            sb.append(escape(f.detail()));
        }
        if (f.snippet() != null) {
            sb.append("<br><code>").append(escape(f.snippet())).append("</code>");
        }
        sb.append("</td></tr>\n");
    }

    static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (char c : Printable.plain(value).toCharArray()) {
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
