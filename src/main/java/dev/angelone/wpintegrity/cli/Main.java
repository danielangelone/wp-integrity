package dev.angelone.wpintegrity.cli;

import dev.angelone.wpintegrity.core.ChecksumClient;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        int code = new WpIntegrityCommand(ChecksumClient.WORDPRESS_ORG, out, err).commandLine().execute(args);
        System.exit(code);
    }
}
