package be.imgn.mtg.tooling.scryfall;

import java.io.IOException;
import java.util.function.LongConsumer;

import okio.Buffer;
import okio.ForwardingSource;
import okio.Source;

/// A Source wrapper that reports bytes read to a progress callback.
final class ProgressSource extends ForwardingSource {

    private final LongConsumer progressCallback;
    private long totalBytesRead;

    ProgressSource(Source delegate, LongConsumer progressCallback) {
        super(delegate);
        this.progressCallback = progressCallback;
        this.totalBytesRead = 0;
    }

    @Override
    public long read(Buffer sink, long byteCount) throws IOException {
        long bytesRead = super.read(sink, byteCount);
        if (bytesRead != -1) {
            totalBytesRead += bytesRead;
            progressCallback.accept(totalBytesRead);
        }
        return bytesRead;
    }
}
