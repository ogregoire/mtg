package be.imgn.mtg.tooling.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import be.imgn.mtg.tooling.db.H2Database;
import be.imgn.mtg.tooling.db.ToolsConfig;

/// SQL query command for ad-hoc read-only database access.
///
/// Usage:
/// ```
/// mtg sql <query>
/// mtg sql --no-truncate <query>
/// ```
public final class SqlCommand {

    private static final int DEFAULT_MAX_WIDTH = 60;
    private static final Set<String> ALLOWED_PREFIXES = Set.of("SELECT", "SHOW", "EXPLAIN");

    private SqlCommand() {}

    /// Runs the SQL command with the given arguments.
    ///
    /// @param args command-line arguments
    public static void run(List<String> args) {
        if (args.isEmpty() || args.contains("-h") || args.contains("--help") || args.contains("help")) {
            printHelp();
            return;
        }

        var truncate = true;
        var queryArgs = new ArrayList<>(args);

        if (queryArgs.contains("--no-truncate") || queryArgs.contains("-T")) {
            truncate = false;
            queryArgs.remove("--no-truncate");
            queryArgs.remove("-T");
        }

        if (queryArgs.isEmpty()) {
            printHelp();
            return;
        }

        var query = String.join(" ", queryArgs);

        // Read-only enforcement: prefix check
        var firstWord = query.stripLeading().split("\\s+", 2)[0].toUpperCase(Locale.ROOT);
        if (!ALLOWED_PREFIXES.contains(firstWord)) {
            System.err.println("Error: Only SELECT, SHOW, and EXPLAIN queries are allowed.");
            System.exit(1);
            return;
        }

        var config = ToolsConfig.withDefaults();
        try (var db = H2Database.createReadOnly(config)) {
            executeQuery(db, query, truncate);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void executeQuery(H2Database db, String query, boolean truncate) {
        db.jdbi().useHandle(handle -> {
            try (var stmt = handle.getConnection().createStatement();
                    var rs = stmt.executeQuery(query)) {
                printResultSet(rs, truncate);
            } catch (SQLException e) {
                System.err.println("Query error: " + e.getMessage());
                System.exit(1);
            }
        });
    }

    private static void printResultSet(ResultSet rs, boolean truncate) throws SQLException {
        var meta = rs.getMetaData();
        var columnCount = meta.getColumnCount();

        // Collect headers
        var headers = new String[columnCount];
        for (var i = 0; i < columnCount; i++) {
            headers[i] = meta.getColumnLabel(i + 1);
        }

        // Collect all rows
        var rows = new ArrayList<String[]>();
        while (rs.next()) {
            var row = new String[columnCount];
            for (var i = 0; i < columnCount; i++) {
                var value = rs.getString(i + 1);
                row[i] = value != null ? value : "(null)";
            }
            rows.add(row);
        }

        // Calculate column widths
        var widths = new int[columnCount];
        for (var i = 0; i < columnCount; i++) {
            widths[i] = headers[i].length();
        }
        for (var row : rows) {
            for (var i = 0; i < columnCount; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        // Apply truncation
        var maxWidth = truncate ? DEFAULT_MAX_WIDTH : Integer.MAX_VALUE;
        for (var i = 0; i < columnCount; i++) {
            widths[i] = Math.min(widths[i], maxWidth);
        }

        // Print headers
        printRow(headers, widths, maxWidth);

        // Print separator
        var sb = new StringBuilder();
        for (var i = 0; i < columnCount; i++) {
            if (i > 0) sb.append("-+-");
            sb.append("-".repeat(widths[i]));
        }
        System.out.println(sb);

        // Print rows
        for (var row : rows) {
            printRow(row, widths, maxWidth);
        }

        // Print row count
        var count = rows.size();
        System.out.println("(" + count + (count == 1 ? " row)" : " rows)"));
    }

    private static void printRow(String[] values, int[] widths, int maxWidth) {
        var sb = new StringBuilder();
        for (var i = 0; i < values.length; i++) {
            if (i > 0) sb.append(" | ");
            var value = values[i];
            var width = widths[i];
            if (value.length() > maxWidth) {
                value = value.substring(0, maxWidth - 1) + "~";
            }
            sb.append(String.format("%-" + width + "s", value));
        }
        System.out.println(sb);
    }

    private static void printHelp() {
        System.out.println("""
                MTG SQL Query

                Usage:
                  mtg sql [options] <query>

                Options:
                  --no-truncate, -T    Show full column values (default: truncate at 60 chars)

                Tables:
                  card            Oracle cards (name, mana_cost, oracle_text, type_line, ...)
                  card_set        Sets (code, name, type)
                  print           Individual printings (card_id, set_id, rarity, ...)
                  format          Format names (format_id, format_name)
                  legality        Card format legality (card_id, format_id, legality)
                  ruling          Card rulings (card_id, source, published_at, comment)
                  rule            Comprehensive rules (rule_number, text, section)
                  rule_version    Rules version info
                  rule_glossary   Glossary terms (term, definition)
                  rule_keyword    Keyword-to-rule mapping

                Only SELECT, SHOW, and EXPLAIN queries are allowed.

                Examples:
                  mtg sql "SHOW TABLES"
                  mtg sql "SELECT name, mana_cost FROM card LIMIT 5"
                  mtg sql "SELECT name, oracle_text FROM card WHERE name = 'Lightning Bolt'"
                  mtg sql -T "SELECT oracle_text FROM card WHERE name = 'Cryptic Command'"
                  mtg sql "SHOW COLUMNS FROM card"
                """);
    }
}
