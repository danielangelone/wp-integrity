package dev.angelone.wpintegrity.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class ToolInfo {

    private static final String VERSION = load();

    private ToolInfo() {
    }

    public static String version() {
        return VERSION;
    }

    private static String load() {
        try (InputStream in = ToolInfo.class.getResourceAsStream("/wpintegrity.properties")) {
            Properties p = new Properties();
            if (in != null) {
                p.load(in);
            }
            return p.getProperty("version", "unknown");
        } catch (IOException e) {
            return "unknown";
        }
    }
}
