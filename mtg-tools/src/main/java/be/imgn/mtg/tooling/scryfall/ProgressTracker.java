package be.imgn.mtg.tooling.scryfall;

import java.io.PrintStream;

/// Tracks and displays progress with percentage, using carriage return to update in place.
public final class ProgressTracker {

    private static final long KB = 1024L;
    private static final long MB = 1024L * 1024L;
    private static final long GB = 1024L * 1024L * 1024L;

    private final PrintStream out;
    private final String label;
    private final long total;
    private final boolean showPercentage;
    private final boolean formatAsBytes;
    private long current;
    private long lastPrintTime;
    private boolean hasPrinted;
    private static final long MIN_PRINT_INTERVAL_MS = 100; // Don't print more than 10x/second

    /// Creates a tracker for operations with known total.
    public ProgressTracker(String label, long total) {
        this(label, total, System.out, false);
    }

    /// Creates a tracker with custom output stream (for testing).
    ProgressTracker(String label, long total, PrintStream out) {
        this(label, total, out, false);
    }

    /// Creates a tracker with custom output stream and byte formatting option.
    private ProgressTracker(String label, long total, PrintStream out, boolean formatAsBytes) {
        this.out = out;
        this.label = label;
        this.total = total;
        this.showPercentage = total > 0;
        this.formatAsBytes = formatAsBytes;
        this.current = 0;
        this.lastPrintTime = System.currentTimeMillis(); // Delay first print by MIN_PRINT_INTERVAL_MS
        this.hasPrinted = false;
    }

    /// Updates progress and prints status.
    public void update(long current) {
        this.current = current;
        long now = System.currentTimeMillis();
        if (now - lastPrintTime >= MIN_PRINT_INTERVAL_MS) {
            printProgress();
            lastPrintTime = now;
        }
    }

    /// Increments progress by delta.
    public void increment(long delta) {
        update(current + delta);
    }

    /// Marks progress as complete and prints final line with newline.
    public void complete() {
        current = total > 0 ? total : current;
        printComplete();
    }

    /// Marks progress as complete with a custom count and prints final line.
    public void complete(long finalCount) {
        current = finalCount;
        printComplete();
    }

    private void printComplete() {
        // Clear the line only if we printed progress before
        if (hasPrinted) {
            out.print("\r" + " ".repeat(80) + "\r");
        }
        if (formatAsBytes) {
            // Change "Downloading X" to "Downloaded X"
            String completedLabel =
                    label.startsWith("Downloading ") ? "Downloaded " + label.substring("Downloading ".length()) : label;
            out.printf("%s (%s)%n", completedLabel, formatBytes(current));
        } else if (hasPrinted) {
            // Only print completion for non-byte formats if we showed progress
            out.printf("%s: %,d%n", label, current);
        }
        out.flush();
    }

    private void printProgress() {
        hasPrinted = true;
        if (showPercentage) {
            double percentage = total > 0 ? (current * 100.0 / total) : 0;
            if (formatAsBytes) {
                out.printf("\r%s: %s / %s (%.1f%%)", label, formatBytes(current), formatBytes(total), percentage);
            } else {
                out.printf("\r%s: %,d / %,d (%.1f%%)", label, current, total, percentage);
            }
        } else {
            if (formatAsBytes) {
                out.printf("\r%s: %s", label, formatBytes(current));
            } else {
                out.printf("\r%s: %,d", label, current);
            }
        }
        out.flush();
    }

    /// Formats bytes using the most significant unit (B, KB, MB, GB) with 4 significant digits.
    private static String formatBytes(long bytes) {
        if (bytes < KB) {
            return bytes + " B";
        } else if (bytes < MB) {
            return String.format("%.4g KB", bytes / (double) KB);
        } else if (bytes < GB) {
            return String.format("%.4g MB", bytes / (double) MB);
        } else {
            return String.format("%.4g GB", bytes / (double) GB);
        }
    }

    /// Creates a tracker for byte-based download progress with human-readable byte formatting.
    public static ProgressTracker forDownload(String label, long contentLength) {
        return new ProgressTracker(label, contentLength, System.out, true);
    }

    /// Creates a tracker for byte-based download progress with custom output stream.
    static ProgressTracker forDownload(String label, long contentLength, PrintStream out) {
        return new ProgressTracker(label, contentLength, out, true);
    }

    /// Creates a tracker for item-based import progress.
    public static ProgressTracker forImport(String label, long totalItems) {
        return new ProgressTracker(label, totalItems, System.out, false);
    }

    /// Creates a tracker without known total (just shows count).
    public static ProgressTracker forUnknownTotal(String label) {
        return new ProgressTracker(label, -1, System.out, false);
    }
}
