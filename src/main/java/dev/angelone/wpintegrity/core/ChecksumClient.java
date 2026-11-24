package dev.angelone.wpintegrity.core;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Fetches the official core checksums. The base address is a constructor argument only for tests. */
public final class ChecksumClient {

    public static final URI WORDPRESS_ORG = URI.create("https://api.wordpress.org/core/checksums/1.0/");

    private final URI baseUri;
    private final HttpClient http;
    private final String userAgent;

    public ChecksumClient(URI baseUri, String toolVersion) {
        this.baseUri = baseUri;
        this.userAgent = "wp-integrity/" + toolVersion;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    public Checksums fetch(WordPressVersion version) {
        URI uri = URI.create(baseUri + "?version=" + encode(version.version()) + "&locale=" + encode(version.locale()));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", userAgent)
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ScanException("Could not reach the WordPress.org checksums API: " + e, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScanException("Interrupted while fetching the checksums", e);
        }
        if (response.statusCode() != 200) {
            throw new ScanException("The WordPress.org checksums API answered HTTP " + response.statusCode());
        }
        return Checksums.parse(response.body(), version);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
