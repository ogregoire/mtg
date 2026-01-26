package be.imgn.mtg.tooling.rules;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.db.dao.RulesDao;
import be.imgn.mtg.tooling.rules.model.ParsedRules;
import be.imgn.mtg.tooling.scryfall.ProgressTracker;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/// Synchronizes Comprehensive Rules from Wizards of the Coast.
///
/// Downloads the rules TXT file and imports it into the database.
public final class RulesSync {

    // Page containing links to the rules files
    private static final String RULES_PAGE_URL = "https://magic.wizards.com/en/rules";

    // Pattern to find TXT download link in the page
    // Note: URL may contain spaces (e.g., "MagicCompRules 20260116.txt")
    private static final Pattern TXT_LINK_PATTERN =
            Pattern.compile("https://media\\.wizards\\.com/[^\"']+(?:Comp|comp)[Rr]ules[^\"']*\\.txt");

    // H2 Lucene full-text search class (used in CREATE ALIAS statements)
    private static final String H2_FTL_CLASS = "org.h2" + ".fulltext.FullTextLucene";

    private final OkHttpClient client;
    private final Jdbi jdbi;
    private final PrintStream out;

    /// Creates a new RulesSync service.
    ///
    /// @param client the HTTP client
    /// @param jdbi the JDBI instance
    public RulesSync(OkHttpClient client, Jdbi jdbi) {
        this(client, jdbi, System.out);
    }

    /// Creates a new RulesSync service with custom output stream.
    ///
    /// @param client the HTTP client
    /// @param jdbi the JDBI instance
    /// @param out output stream for progress messages
    RulesSync(OkHttpClient client, Jdbi jdbi, PrintStream out) {
        this.client = client;
        this.jdbi = jdbi;
        this.out = out;
    }

    /// Performs a sync of the Comprehensive Rules.
    ///
    /// Attempts to download the latest rules by trying recent dates.
    ///
    /// @throws IOException if the download or import fails
    public void sync() throws IOException {
        var totalStart = System.nanoTime();
        out.println("Syncing Comprehensive Rules from Wizards of the Coast...");
        out.flush();

        // Try to find and download the latest rules
        var rulesDownload = downloadLatestRules();

        out.println("Parsing rules...");
        out.flush();

        var parsedRules = RulesParser.parse(rulesDownload.content());

        out.printf(
                "Found %,d rules, %,d glossary entries, %,d keywords%n",
                parsedRules.rules().size(),
                parsedRules.glossary().size(),
                parsedRules.keywords().size());
        out.flush();

        importRules(parsedRules, rulesDownload.url());

        var totalTime = System.nanoTime() - totalStart;
        out.printf("Rules sync completed successfully in %s%n", formatDuration(totalTime));
    }

    private RulesDownload downloadLatestRules() throws IOException {
        out.println("Fetching rules page...");
        out.flush();

        // Try to get URL from the rules page first
        var pageRequest = new Request.Builder().url(RULES_PAGE_URL).build();

        try (var response = client.newCall(pageRequest).execute()) {
            if (response.isSuccessful()) {
                var body = response.body();
                if (body != null) {
                    var pageContent = body.string();
                    var matcher = TXT_LINK_PATTERN.matcher(pageContent);
                    if (matcher.find()) {
                        var foundUrl = matcher.group().replace(" ", "%20");
                        out.println("Found rules URL: " + foundUrl);
                        out.flush();

                        // Try to download from the page URL
                        var content = tryDownload(foundUrl);
                        if (content != null) {
                            return new RulesDownload(foundUrl, content);
                        }
                        out.println("URL from rules page not accessible, trying date-based fallback...");
                        out.flush();
                    }
                }
            }
        }

        // Fallback: try date-based URL patterns
        out.println("Searching for rules by date...");
        out.flush();

        var today = LocalDate.now(ZoneId.systemDefault());
        var formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        for (var daysBack = 0; daysBack <= 365; daysBack++) {
            var date = today.minusDays(daysBack);
            var dateStr = date.format(formatter);
            var year = date.getYear();

            var url = String.format("https://media.wizards.com/%d/downloads/MagicCompRules%%20%s.txt", year, dateStr);
            var content = tryDownload(url);
            if (content != null) {
                return new RulesDownload(url, content);
            }
        }

        throw new IOException("Could not find Comprehensive Rules file. " + "Please check " + RULES_PAGE_URL
                + " for the latest rules.");
    }

    private @Nullable String tryDownload(String url) throws IOException {
        var request = new Request.Builder().url(url).head().build();

        // First check if the URL exists with HEAD request
        try (var response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
        }

        // URL exists, download the content
        var getRequest = new Request.Builder().url(url).build();

        try (var response = client.newCall(getRequest).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }
            var body = response.body();
            if (body == null) {
                return null;
            }

            var contentLength = body.contentLength();
            var tracker = ProgressTracker.forDownload("Downloading rules", contentLength);

            // Read the content with progress tracking
            var source = body.source();
            var buffer = new byte[8192];
            var result = new StringBuilder();
            long totalRead = 0;

            while (true) {
                var read = source.read(buffer);
                if (read == -1) break;
                result.append(new String(buffer, 0, read, StandardCharsets.UTF_8));
                totalRead += read;
                tracker.update(totalRead);
            }

            tracker.complete();
            return result.toString();
        }
    }

    private void importRules(ParsedRules parsedRules, String sourceUrl) {
        var startTime = System.nanoTime();

        jdbi.useHandle(handle -> {
            var rulesDao = handle.attach(RulesDao.class);

            // Drop indexes for faster bulk insert
            rulesDao.dropRuleNumberIndex();
            rulesDao.dropRuleParentIndex();
            rulesDao.dropRuleSectionIndex();

            // Clear existing data
            rulesDao.deleteAllKeywords();
            rulesDao.deleteAllGlossary();
            rulesDao.deleteAllRules();
            rulesDao.deleteAllVersions();

            // Insert version info
            rulesDao.insertVersion(
                    parsedRules.effectiveDate().toString(),
                    parsedRules.effectiveDate(),
                    LocalDateTime.now(ZoneId.systemDefault()),
                    sourceUrl);

            // Insert rules
            var tracker = ProgressTracker.forImport(
                    "Importing rules", parsedRules.rules().size());
            var count = 0;
            for (var rule : parsedRules.rules()) {
                rulesDao.insertRule(
                        rule.ruleNumber(), rule.text(), rule.parentRule(), rule.section(), rule.sectionNumber());
                count++;
                if (count % 100 == 0) {
                    tracker.update(count);
                }
            }
            tracker.complete(count);

            // Insert glossary entries
            out.printf(
                    "Importing %,d glossary entries...%n",
                    parsedRules.glossary().size());
            out.flush();
            for (var entry : parsedRules.glossary()) {
                rulesDao.insertGlossary(entry.term(), entry.definition());
            }

            // Insert keywords
            out.printf("Importing %,d keywords...%n", parsedRules.keywords().size());
            out.flush();
            for (var entry : parsedRules.keywords().entrySet()) {
                rulesDao.insertKeyword(entry.getKey(), entry.getValue());
            }

            // Recreate indexes
            rulesDao.createRuleNumberIndex();
            rulesDao.createRuleParentIndex();
            rulesDao.createRuleSectionIndex();

            // Create Lucene full-text search indexes
            out.println("Creating full-text search indexes...");
            out.flush();
            initializeLuceneSearch(handle);
        });

        out.printf("Import completed in %s%n", formatDuration(System.nanoTime() - startTime));
    }

    private void initializeLuceneSearch(Handle handle) {
        // Initialize Lucene full-text search
        handle.execute("CREATE ALIAS IF NOT EXISTS FTL_INIT FOR '" + H2_FTL_CLASS + ".init'");
        handle.execute("CALL FTL_INIT()");

        // Drop existing indexes (if any) and recreate
        handle.execute("CREATE ALIAS IF NOT EXISTS FTL_DROP_ALL FOR '" + H2_FTL_CLASS + ".dropAll'");
        try {
            handle.execute("CALL FTL_DROP_ALL()");
        } catch (Exception e) {
            // Ignore if no indexes exist
        }

        // Create full-text index on rules text
        handle.execute("CREATE ALIAS IF NOT EXISTS FTL_CREATE_INDEX FOR '" + H2_FTL_CLASS + ".createIndex'");
        handle.execute("CALL FTL_CREATE_INDEX('PUBLIC', 'RULE', 'TEXT')");

        // Create full-text index on glossary
        handle.execute("CALL FTL_CREATE_INDEX('PUBLIC', 'RULE_GLOSSARY', 'TERM,DEFINITION')");
    }

    private static String formatDuration(long nanos) {
        var millis = nanos / 1_000_000;
        if (millis < 1000) {
            return millis + " ms";
        }
        var seconds = millis / 1000.0;
        if (seconds < 60) {
            return String.format("%.2f s", seconds);
        }
        var mins = (long) (seconds / 60);
        var secs = seconds % 60;
        return String.format("%d min %.2f s", mins, secs);
    }

    private record RulesDownload(String url, String content) {}
}
