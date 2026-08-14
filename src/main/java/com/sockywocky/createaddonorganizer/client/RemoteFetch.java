package com.sockywocky.createaddonorganizer.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sockywocky.createaddonorganizer.createaddonorganizer;

public final class RemoteFetch {

    private RemoteFetch() {}

    public record FetchResult(boolean notModified, byte[] body, String etag, String baseUrl) {}

    public static HttpClient newClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public static void startDaemon(String threadName, Runnable body, AtomicBoolean guard) {
        if (!guard.compareAndSet(false, true)) {
            return;
        }
        Thread thread = new Thread(body, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    public static FetchResult fetchManifest(HttpClient client, String url, String etag, String logContext) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET();
            if (etag != null && !etag.isBlank()) {
                builder.header("If-None-Match", etag);
            }
            HttpResponse<byte[]> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            String baseUrl = url.substring(0, url.lastIndexOf('/') + 1);
            if (response.statusCode() == 304) {
                return new FetchResult(true, null, etag, baseUrl);
            }
            if (response.statusCode() != 200) {
                return null;
            }
            String newEtag = response.headers().firstValue("ETag").orElse(null);
            return new FetchResult(false, response.body(), newEtag, baseUrl);
        } catch (IOException | InterruptedException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to fetch remote {} from {}", logContext, url, e);
            return null;
        }
    }

    public static FetchResult fetchWithFallback(HttpClient client, String primaryUrl, String fallbackUrl,
            String etag, String logContext) {
        FetchResult result = fetchManifest(client, primaryUrl, etag, logContext);
        if (result == null) {
            result = fetchManifest(client, fallbackUrl, etag, logContext);
        }
        return result;
    }

    public static String readEtag(Path etagFile, String logContext) {
        try {
            if (Files.exists(etagFile)) {
                return Files.readString(etagFile, StandardCharsets.UTF_8).trim();
            }
        } catch (IOException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to read cached {} etag", logContext, e);
        }
        return null;
    }

    public static void writeAtomic(Path target, byte[] bytes) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
