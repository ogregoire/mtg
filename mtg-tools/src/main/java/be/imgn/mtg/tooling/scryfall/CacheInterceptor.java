package be.imgn.mtg.tooling.scryfall;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Okio;

/// Disk-based cache interceptor for HTTP responses.
///
/// Caches responses to disk with time-based expiration.
/// Designed for Scryfall bulk data which should be refreshed daily.
public final class CacheInterceptor implements Interceptor {

    private static final long DEFAULT_MAX_AGE_HOURS = 24;

    private final Path cacheDir;
    private final long maxAgeMillis;

    /// Creates a CacheInterceptor with default 24-hour max age.
    ///
    /// @param cacheDir directory to store cached responses
    public CacheInterceptor(Path cacheDir) {
        this(cacheDir, DEFAULT_MAX_AGE_HOURS, TimeUnit.HOURS);
    }

    /// Creates a CacheInterceptor with custom max age.
    ///
    /// @param cacheDir directory to store cached responses
    /// @param maxAge maximum age for cached entries
    /// @param unit time unit for maxAge
    public CacheInterceptor(Path cacheDir, long maxAge, TimeUnit unit) {
        this.cacheDir = cacheDir;
        this.maxAgeMillis = unit.toMillis(maxAge);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        var request = chain.request();

        // Only cache GET requests
        if (!"GET".equals(request.method())) {
            return chain.proceed(request);
        }

        ensureCacheDirectory();
        var cacheFile = cacheDir.resolve(getCacheKey(request.url().toString()));

        // Check if we have a valid cached response
        if (Files.exists(cacheFile)) {
            if (!isExpired(cacheFile)) {
                return buildCachedResponse(request, cacheFile);
            }

            // Cache expired - send conditional request using file's last modified time
            var lastModified = Files.getLastModifiedTime(cacheFile).toInstant();
            var httpDate = DateTimeFormatter.RFC_1123_DATE_TIME
                    .withZone(ZoneOffset.UTC)
                    .format(lastModified);

            var conditionalRequest =
                    request.newBuilder().header("If-Modified-Since", httpDate).build();

            var networkResponse = chain.proceed(conditionalRequest);

            // 304 Not Modified - use cached content and reset expiration
            if (networkResponse.code() == 304) {
                networkResponse.close();
                Files.setLastModifiedTime(cacheFile, FileTime.fromMillis(System.currentTimeMillis()));
                return buildCachedResponse(request, cacheFile);
            }

            // New content - cache it
            if (networkResponse.isSuccessful()) {
                return buildTeeResponse(networkResponse, cacheFile);
            }

            return networkResponse;
        }

        var networkResponse = chain.proceed(request);

        // Cache successful responses - stream through to caller while caching
        if (networkResponse.isSuccessful()) {
            return buildTeeResponse(networkResponse, cacheFile);
        }

        return networkResponse;
    }

    private void ensureCacheDirectory() throws IOException {
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }
    }

    private String getCacheKey(String url) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            var hash = md.digest(url.getBytes(StandardCharsets.UTF_8));
            var hashHex = String.format("%064x", new BigInteger(1, hash)).substring(0, 16);

            // Extract path from URL for readability
            var path = extractPath(url);
            return path.isEmpty() ? hashHex : hashHex + "_" + path;
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String extractPath(String url) {
        // Find the path after the domain (after "://host/")
        var protocolEnd = url.indexOf("://");
        if (protocolEnd < 0) return "";

        var pathStart = url.indexOf('/', protocolEnd + 3);
        if (pathStart < 0) return "";

        var path = url.substring(pathStart + 1); // Skip the leading slash
        // Replace unsafe chars for filenames
        return path.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private boolean isExpired(Path cacheFile) throws IOException {
        var lastModified = Files.getLastModifiedTime(cacheFile).toInstant();
        var expiration = lastModified.plusMillis(maxAgeMillis);
        return Instant.now().isAfter(expiration);
    }

    private Response buildTeeResponse(Response response, Path cacheFile) throws IOException {
        var body = response.body();
        if (body == null) {
            return response;
        }

        // Create a tee source that writes to cache while streaming to caller
        var cacheSink = Okio.buffer(Okio.sink(cacheFile));
        var teeSource = new TeeSource(body.source(), cacheSink);
        var teeBody = ResponseBody.create(Okio.buffer(teeSource), body.contentType(), body.contentLength());

        return response.newBuilder().body(teeBody).build();
    }

    /// A Source that writes all read data to a sink (tee/pipe pattern).
    private static class TeeSource implements okio.Source {
        private final okio.Source source;
        private final okio.BufferedSink sink;

        TeeSource(okio.Source source, okio.BufferedSink sink) {
            this.source = source;
            this.sink = sink;
        }

        @Override
        public long read(okio.Buffer buffer, long byteCount) throws IOException {
            long bytesRead = source.read(buffer, byteCount);
            if (bytesRead > 0) {
                // Copy the read bytes to the cache sink
                buffer.copyTo(sink.getBuffer(), buffer.size() - bytesRead, bytesRead);
                sink.emitCompleteSegments();
            } else if (bytesRead == -1) {
                // End of stream - close the cache file
                sink.close();
            }
            return bytesRead;
        }

        @Override
        public okio.Timeout timeout() {
            return source.timeout();
        }

        @Override
        public void close() throws IOException {
            source.close();
            sink.close();
        }
    }

    private Response buildCachedResponse(Request request, Path cacheFile) throws IOException {
        var content = Files.readAllBytes(cacheFile);
        var body = ResponseBody.create(content, MediaType.parse("application/json"));

        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK (cached)")
                .body(body)
                .build();
    }
}
