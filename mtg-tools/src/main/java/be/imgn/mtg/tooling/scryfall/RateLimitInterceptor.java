package be.imgn.mtg.tooling.scryfall;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

import okhttp3.Interceptor;
import okhttp3.Response;

/// OkHttp interceptor that enforces rate limiting between requests.
///
/// Scryfall API requires at least 100ms between requests to avoid
/// being rate-limited. This interceptor ensures compliance by
/// tracking the last request time and delaying if necessary.
final class RateLimitInterceptor implements Interceptor {

    private static final long MIN_DELAY_NANOS = 100_000_000L; // 100ms

    private long lastRequestTimeNanos;

    @Override
    public Response intercept(Chain chain) throws IOException {
        synchronized (this) {
            var now = System.nanoTime();
            var elapsed = now - lastRequestTimeNanos;

            if (elapsed < MIN_DELAY_NANOS && lastRequestTimeNanos != 0) {
                var sleepNanos = MIN_DELAY_NANOS - elapsed;
                LockSupport.parkNanos(sleepNanos);
            }

            lastRequestTimeNanos = System.nanoTime();
        }

        return chain.proceed(chain.request());
    }
}
