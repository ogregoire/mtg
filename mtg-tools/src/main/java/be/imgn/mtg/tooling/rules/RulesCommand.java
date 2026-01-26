package be.imgn.mtg.tooling.rules;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.db.H2Database;
import be.imgn.mtg.tooling.db.ToolsConfig;
import be.imgn.mtg.tooling.db.dao.RulesQueryDao;
import okhttp3.OkHttpClient;

/// Rules lookup and search command.
///
/// Usage:
/// ```
/// mtg rules [query] [options]
/// ```
///
/// Options:
/// - `--search <query>` - Full-text search in rules
/// - `--glossary <term>` - Look up a glossary term
/// - `--extended` - Show cross-references for keyword lookups
/// - `--sync` - Force re-sync from Wizards
/// - `--version` - Show current rules version
public final class RulesCommand {

    private static final int MAX_SEARCH_RESULTS = 20;

    // Pattern for rule numbers: 100, 100.1, 100.1a, 702.9ab, etc.
    private static final Pattern RULE_NUMBER_PATTERN = Pattern.compile("^\\d{3}(?:\\.\\d+[a-z]*)?$");

    // Pattern for section numbers: 100, 702, etc.
    private static final Pattern SECTION_NUMBER_PATTERN = Pattern.compile("^\\d{3}$");

    private RulesCommand() {}

    /// Runs the rules command with the given arguments.
    ///
    /// @param args command-line arguments
    public static void run(List<String> args) {
        if (args.contains("-h") || args.contains("--help") || args.contains("help")) {
            printHelp();
            return;
        }

        var options = parseOptions(args);
        if (options == null) {
            System.exit(1);
            return;
        }

        var config = ToolsConfig.withDefaults();
        try (var db = H2Database.create(config)) {
            var dao = new RulesQueryDao(db.jdbi());

            // Handle --sync flag
            if (options.sync) {
                performSync(db);
                if (options.query == null && options.search == null && options.glossary == null && !options.version) {
                    return; // Just sync, nothing else
                }
            }

            // Check if rules are synced
            if (!dao.hasRules()) {
                System.out.println("No rules found. Syncing from Wizards of the Coast...");
                performSync(db);
            }

            // Handle --version flag
            if (options.version) {
                var version = dao.getVersion();
                if (version.isPresent()) {
                    System.out.print(RulesFormatter.formatVersion(version.get()));
                } else {
                    System.err.println("No rules version information found.");
                }
                return;
            }

            // Handle --glossary flag
            if (options.glossary != null) {
                handleGlossary(dao, options.glossary);
                return;
            }

            // Handle --search flag
            if (options.search != null) {
                handleSearch(dao, options.search);
                return;
            }

            // Handle positional query
            if (options.query != null) {
                handleQuery(dao, options.query, options.extended);
                return;
            }

            // No query provided - show help
            printHelp();
        }
    }

    private static void performSync(H2Database db) {
        try {
            var client = new OkHttpClient.Builder().build();
            var sync = new RulesSync(client, db.jdbi());
            sync.sync();
        } catch (IOException e) {
            System.err.println("Failed to sync rules: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void handleGlossary(RulesQueryDao dao, String term) {
        // Try exact match first
        var exact = dao.findGlossaryByTerm(term);
        if (exact.isPresent()) {
            System.out.print(RulesFormatter.formatGlossary(exact.get()));
            return;
        }

        // Try search
        var results = dao.searchGlossary(term, MAX_SEARCH_RESULTS);
        if (results.isEmpty()) {
            System.out.println("No glossary entry found for \"" + term + "\".");
        } else if (results.size() == 1) {
            System.out.print(RulesFormatter.formatGlossary(results.getFirst()));
        } else {
            System.out.print(RulesFormatter.formatGlossaryResults(term, results));
        }
    }

    private static void handleSearch(RulesQueryDao dao, String query) {
        var results = dao.searchRules(query, MAX_SEARCH_RESULTS);
        System.out.print(RulesFormatter.formatSearchResults(query, results));
    }

    private static void handleQuery(RulesQueryDao dao, String query, boolean extended) {
        // Check if it's a rule number
        if (RULE_NUMBER_PATTERN.matcher(query).matches()) {
            handleRuleNumber(dao, query, extended);
            return;
        }

        // Check if it's a section number
        if (SECTION_NUMBER_PATTERN.matcher(query).matches()) {
            handleSectionNumber(dao, query);
            return;
        }

        // Try as keyword
        handleKeyword(dao, query, extended);
    }

    private static void handleRuleNumber(RulesQueryDao dao, String ruleNumber, boolean extended) {
        // Get the rule and its sub-rules
        var rules = dao.findRuleWithSubRules(ruleNumber);

        if (rules.isEmpty()) {
            System.out.println("No rule found for \"" + ruleNumber + "\".");
            return;
        }

        if (extended) {
            var crossRefs = dao.findCrossReferences(ruleNumber, 10);
            System.out.print(RulesFormatter.formatRulesExtended(rules, crossRefs, null));
        } else {
            System.out.print(RulesFormatter.formatRules(rules, false, null));
        }
    }

    private static void handleSectionNumber(RulesQueryDao dao, String sectionNumber) {
        var rules = dao.findBySection(sectionNumber);

        if (rules.isEmpty()) {
            System.out.println("No rules found for section " + sectionNumber + ".");
            return;
        }

        // Show all rules in the section
        System.out.print(RulesFormatter.formatSection(rules));
    }

    private static void handleKeyword(RulesQueryDao dao, String keyword, boolean extended) {
        // Try to find the keyword in the keyword mapping
        var ruleNumber = dao.findKeywordRuleNumber(keyword);

        if (ruleNumber.isPresent()) {
            // Found as keyword - get the rule and sub-rules
            var rules = dao.findRuleWithSubRules(ruleNumber.get());

            if (extended) {
                var crossRefs = dao.findCrossReferences(ruleNumber.get(), 10);
                System.out.print(RulesFormatter.formatRulesExtended(rules, crossRefs, keyword));
            } else {
                System.out.print(RulesFormatter.formatRules(rules, true, keyword));
            }
            return;
        }

        // Not found as keyword - try as search
        System.out.println("Keyword \"" + keyword + "\" not found. Searching rules...\n");
        var results = dao.searchRules(keyword, MAX_SEARCH_RESULTS);
        System.out.print(RulesFormatter.formatSearchResults(keyword, results));
    }

    private static @Nullable Options parseOptions(List<String> args) {
        var options = new Options();

        for (var i = 0; i < args.size(); i++) {
            var arg = args.get(i);
            switch (arg) {
                case "--search", "-s" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --search requires a value");
                        return null;
                    }
                    options.search = args.get(++i);
                }
                case "--glossary", "-g" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --glossary requires a value");
                        return null;
                    }
                    options.glossary = args.get(++i);
                }
                case "--extended", "-e" -> options.extended = true;
                case "--sync" -> options.sync = true;
                case "--version", "-v" -> options.version = true;
                default -> {
                    if (arg.startsWith("-")) {
                        System.err.println("Unknown option: " + arg);
                        System.err.println("Run 'mtg rules --help' for usage.");
                        return null;
                    }
                    // Positional argument
                    if (options.query == null) {
                        options.query = arg;
                    } else {
                        // Multiple words in query - join them
                        options.query = options.query + " " + arg;
                    }
                }
            }
        }

        return options;
    }

    private static void printHelp() {
        System.out.println("""
                MTG Comprehensive Rules

                Usage:
                  mtg rules [query] [options]

                Options:
                  --search, -s <query>   Full-text search in rules
                  --glossary, -g <term>  Look up a glossary term
                  --extended, -e         Show cross-references for keyword lookups
                  --sync                 Force re-sync from Wizards of the Coast
                  --version, -v          Show current rules version info

                Query Types:
                  Rule number            Show rule and all sub-rules (e.g., 702.9, 104.3a)
                  Section number         Show section overview (e.g., 702)
                  Keyword name           Show keyword ability rules (e.g., flying, trample)

                Examples:
                  mtg rules 702.9
                      Show the Flying rule and all sub-rules

                  mtg rules 104.3a
                      Show specific sub-rule

                  mtg rules 702
                      Show Keyword Abilities section overview

                  mtg rules flying
                      Show Flying rules (702.9, 702.9a, 702.9b, etc.)

                  mtg rules trample --extended
                      Show Trample rules with cross-references

                  mtg rules --search "damage"
                      Search for rules containing "damage"

                  mtg rules --search "enters the battlefield"
                      Search for rules about ETB triggers

                  mtg rules --glossary "mana value"
                      Look up "mana value" in the glossary

                  mtg rules --glossary "permanent"
                      Look up "permanent" in the glossary

                  mtg rules --sync
                      Force re-sync rules from Wizards

                  mtg rules --version
                      Show current rules version
                """);
    }

    private static class Options {
        @Nullable
        String query;

        @Nullable
        String search;

        @Nullable
        String glossary;

        boolean extended = false;
        boolean sync = false;
        boolean version = false;
    }
}
