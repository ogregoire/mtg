package be.imgn.mtg.tooling.scryfall;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/// Adds required headers for the Scryfall API.
///
/// Scryfall requires User-Agent and Accept headers on all requests.
final class ScryfallHeaderInterceptor implements Interceptor {

    @Override
    public Response intercept(Chain chain) throws IOException {
        var request = chain.request()
                .newBuilder()
                .header("User-Agent", "mtg-engine/1.0")
                .header("Accept", "application/json")
                .build();
        return chain.proceed(request);
    }
}
