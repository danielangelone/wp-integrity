package dev.angelone.wpintegrity.cli;

import dev.angelone.wpintegrity.core.ChecksumClient;
import dev.angelone.wpintegrity.core.ChecksumFile;
import dev.angelone.wpintegrity.core.Checksums;
import dev.angelone.wpintegrity.core.ScanException;
import dev.angelone.wpintegrity.core.ScanResult;
import dev.angelone.wpintegrity.core.Scanner;
import dev.angelone.wpintegrity.core.ToolInfo;
import dev.angelone.wpintegrity.core.VersionReader;
import dev.angelone.wpintegrity.core.WordPressVersion;
import dev.angelone.wpintegrity.report.ReportFormat;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "wp-integrity", mixinStandardHelpOptions = true, versionProvider = WpIntegrityCommand.Version.class,
        description = "Read-only check of a WordPress installation for signs of tampering.",
        exitCodeListHeading = "%nExit codes:%n",
        exitCodeList = {"0:Nothing at the requested level", "1:Findings at the requested level or above",
                "2:Usage or runtime error"})
public final class WpIntegrityCommand implements Callable<Integer> {

    static final class Version implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] {"wp-integrity " + ToolInfo.version()};
        }
    }

    @Parameters(index = "0", paramLabel = "<wordpress-path>", description = "Root of the WordPress installation")
    private Path wordpressPath;

    @Option(names = "--format", paramLabel = "text|json|html", defaultValue = "text",
            description = "Report format (default: ${DEFAULT-VALUE})")
    private ReportFormat format;

    @Option(names = "--output", paramLabel = "<file>",
            description = "Write the report to this file; required for html; must be outside the scanned folder")
    private Path output;

    @Option(names = "--checksums", paramLabel = "<file.json>",
            description = "Use a saved api.wordpress.org response instead of calling the API")
    private Path checksumsFile;

    @Option(names = "--wp-version", paramLabel = "<v>",
            description = "WordPress version, when wp-includes/version.php cannot be read")
    private String wpVersion;

    @Option(names = "--locale", paramLabel = "<l>",
            description = "WordPress locale, when wp-includes/version.php cannot be read")
    private String locale;

    @Option(names = "--fail-on", paramLabel = "none|warning|error", defaultValue = "error",
            description = "Lowest severity that makes the exit code 1 (default: ${DEFAULT-VALUE})")
    private FailOn failOn;

    @Option(names = "--scan-plugins", description = "Also search plugins and themes for suspicious patterns")
    private boolean scanPlugins;

    private final URI apiBase;
    private final PrintStream out;
    private final PrintStream err;

    /** The API address is a parameter so tests can point at a local server; there is no command line option. */
    public WpIntegrityCommand(URI apiBase, PrintStream out, PrintStream err) {
        this.apiBase = apiBase;
        this.out = out;
        this.err = err;
    }

    public CommandLine commandLine() {
        return new CommandLine(this)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setOut(new java.io.PrintWriter(out, true))
                .setErr(new java.io.PrintWriter(err, true));
    }

    @Override
    public Integer call() {
        try {
            return run();
        } catch (ScanException | IOException e) {
            err.println("wp-integrity: " + e.getMessage());
            return CommandLine.ExitCode.USAGE;
        }
    }

    private int run() throws IOException {
        if (!Files.isDirectory(wordpressPath)) {
            throw new ScanException(wordpressPath + " is not a directory");
        }
        Path root = wordpressPath.toRealPath();
        if (format == ReportFormat.HTML && output == null) {
            throw new ScanException("--output is required for --format html");
        }
        Path target = output == null ? null : checkedOutput(root);

        WordPressVersion version = new VersionReader().read(root, Optional.ofNullable(wpVersion),
                Optional.ofNullable(locale));
        Checksums checksums = checksumsFile != null
                ? new ChecksumFile().load(checksumsFile, version)
                : new ChecksumClient(apiBase, ToolInfo.version()).fetch(version);

        ScanResult result = Scanner.standard().scan(root, version, checksums, scanPlugins, ToolInfo.version());
        String report = format.render(result);
        if (target == null) {
            out.print(report);
        } else {
            Files.writeString(target, report, StandardCharsets.UTF_8);
        }
        return failOn.tripsOn(result) ? 1 : 0;
    }

    /** Refuses a report file that would land inside the scanned folder, links included. */
    private Path checkedOutput(Path root) throws IOException {
        Path absolute = output.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent == null || !Files.isDirectory(parent)) {
            throw new ScanException("The folder for --output does not exist: " + absolute);
        }
        Path target = parent.toRealPath().resolve(absolute.getFileName());
        if (Files.exists(target)) {
            target = target.toRealPath();
        }
        if (target.startsWith(root)) {
            throw new ScanException("--output must be outside the scanned folder: " + target);
        }
        return target;
    }
}
