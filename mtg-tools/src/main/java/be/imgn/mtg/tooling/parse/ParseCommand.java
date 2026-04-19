package be.imgn.mtg.tooling.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jdbi.v3.core.Jdbi;
import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle.OracleParser;
import be.imgn.mtg.tooling.db.H2Database;
import be.imgn.mtg.tooling.db.ToolsConfig;

/// Oracle text parsing command.
///
/// Subcommands:
/// - `all` — parses all cards with `oracle_parsed = false`
/// - `<Card Name>` — parses one card by exact name, prints AST or exception
/// - `reset` — sets `oracle_parsed = false` for all cards
public final class ParseCommand {

    private ParseCommand() {}

    /// Restricts queries to cards that are currently legal or restricted in
    /// Vintage. Only these are considered by the parse commands.
    private static final String VINTAGE_LEGAL_FILTER = " AND card_id IN (SELECT l.card_id FROM legality l"
            + " JOIN format f ON l.format_id = f.format_id"
            + " WHERE f.format_name = 'vintage' AND l.legality IN ('legal', 'restricted'))";

    public static void run(List<String> args) {
        if (args.isEmpty()) {
            printHelp();
            return;
        }

        var first = args.getFirst();
        switch (first) {
            case "-h", "--help", "help" -> printHelp();
            case "all" -> runAll();
            case "reset" -> runReset();
            case "status" -> runStatus();
            case "unparsed" -> runUnparsed(args.size() > 1 ? parsePositiveInt(args.get(1)) : 20);
            // Single-name: detailed (oracle text + AST, updates oracle_parsed).
            // Multi-name: one line per card, `ok` or `fail: <error>`, no DB update.
            default -> {
                if (args.size() == 1) {
                    runOne(args.getFirst());
                } else {
                    runMany(args);
                }
            }
        }
    }

    private static int parsePositiveInt(String s) {
        try {
            var n = Integer.parseInt(s);
            if (n <= 0) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            System.err.println("Expected a positive integer, got: " + s);
            System.exit(1);
            return 0;
        }
    }

    private static void runUnparsed(int limit) {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var rows = db.jdbi()
                    .withHandle(h -> h.createQuery("SELECT name, LENGTH(oracle_text) AS len, oracle_text FROM card"
                                    + " WHERE oracle_parsed = FALSE AND oracle_text IS NOT NULL"
                                    + VINTAGE_LEGAL_FILTER
                                    + " ORDER BY LENGTH(oracle_text) ASC, name ASC LIMIT :limit")
                            .bind("limit", limit)
                            .map((rs, ctx) ->
                                    new Object[] {rs.getString("name"), rs.getInt("len"), rs.getString("oracle_text")})
                            .list());

            if (rows.isEmpty()) {
                System.out.println("No unparsed cards.");
                return;
            }

            var nameWidth = Math.max(
                    4,
                    rows.stream().mapToInt(r -> ((String) r[0]).length()).max().orElse(4));
            var lenWidth = Math.max(
                    3,
                    rows.stream()
                            .mapToInt(r -> String.valueOf(r[1]).length())
                            .max()
                            .orElse(3));

            var fmt = "%-" + nameWidth + "s | %-" + lenWidth + "s | %s%n";
            System.out.printf(Locale.ROOT, fmt, "NAME", "LEN", "ORACLE_TEXT");
            System.out.println("-".repeat(nameWidth) + "-+-" + "-".repeat(lenWidth) + "-+-" + "-".repeat(20));
            for (var r : rows) {
                System.out.printf(Locale.ROOT, fmt, r[0], r[1], ((String) r[2]).replace("\n", " ⏎ "));
            }
        }
    }

    private static void runStatus() {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var row = db.jdbi().withHandle(h -> h.createQuery("SELECT COUNT(*) AS total,"
                            + " SUM(CASE WHEN oracle_parsed THEN 1 ELSE 0 END) AS parsed"
                            + " FROM card WHERE TRUE"
                            + VINTAGE_LEGAL_FILTER)
                    .map((rs, ctx) -> new long[] {rs.getLong("total"), rs.getLong("parsed")})
                    .one());
            var total = row[0];
            var parsed = row[1];
            var remaining = total - parsed;
            var pct = total == 0 ? 0.0 : (100.0 * parsed) / total;
            System.out.printf(Locale.ROOT, "Parsed:    %d / %d (%.2f%%)%n", parsed, total, pct);
            System.out.printf(Locale.ROOT, "Remaining: %d%n", remaining);
        }
    }

    private static void runAll() {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var jdbi = db.jdbi();
            var rows = fetchAllVintage(jdbi);
            System.out.println("Parsing " + rows.size() + " cards...");

            var success = 0;
            var failure = 0;
            var okBatch = new ArrayList<Long>(256);
            var failBatch = new ArrayList<Long>(256);
            for (var row : rows) {
                if (tryParseCard(row)) {
                    okBatch.add(row.cardId());
                    success++;
                    if (okBatch.size() >= 500) {
                        markParsed(jdbi, okBatch);
                        okBatch.clear();
                    }
                } else {
                    failBatch.add(row.cardId());
                    failure++;
                    if (failBatch.size() >= 500) {
                        markUnparsed(jdbi, failBatch);
                        failBatch.clear();
                    }
                }
            }
            if (!okBatch.isEmpty()) {
                markParsed(jdbi, okBatch);
            }
            if (!failBatch.isEmpty()) {
                markUnparsed(jdbi, failBatch);
            }

            var total = success + failure;
            var pct = total == 0 ? 0.0 : (100.0 * success) / total;
            System.out.printf(Locale.ROOT, "Done. Success: %d, failures: %d (%.2f%%)%n", success, failure, pct);
        }
    }

    private static void runReset() {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var updated = db.jdbi().withHandle(h -> h.createUpdate(
                            "UPDATE card SET oracle_parsed = FALSE WHERE TRUE" + VINTAGE_LEGAL_FILTER)
                    .execute());
            System.out.println("Reset oracle_parsed on " + updated + " cards.");
        }
    }

    private static void runOne(String cardName) {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var row = db.jdbi().withHandle(h -> h.createQuery("""
                            SELECT card_id, name, oracle_text,
                                   face_1_name, face_1_oracle_text,
                                   face_2_name, face_2_oracle_text
                            FROM card
                            WHERE name = :name OR face_1_name = :name OR face_2_name = :name
                            LIMIT 1
                            """)
                    .bind("name", cardName)
                    .map((rs, ctx) -> new CardRow(
                            rs.getLong("card_id"),
                            rs.getString("name"),
                            rs.getString("oracle_text"),
                            rs.getString("face_1_name"),
                            rs.getString("face_1_oracle_text"),
                            rs.getString("face_2_name"),
                            rs.getString("face_2_oracle_text")))
                    .findOne());

            if (row.isEmpty()) {
                System.err.println("No card found with exact name: " + cardName);
                System.exit(1);
                return;
            }

            var card = row.get();
            System.out.println("Card: " + card.name());

            var ok = true;
            ok &= parseAndPrint("oracle_text", card.name(), card.oracleText());
            if (card.face1Name() != null) {
                ok &= parseAndPrint("face_1_oracle_text", card.face1Name(), card.face1OracleText());
            }
            if (card.face2Name() != null) {
                ok &= parseAndPrint("face_2_oracle_text", card.face2Name(), card.face2OracleText());
            }

            if (ok) {
                db.jdbi().useHandle(h -> h.createUpdate("UPDATE card SET oracle_parsed = TRUE WHERE card_id = :id")
                        .bind("id", card.cardId())
                        .execute());
            }
        }
    }

    private static void runMany(List<String> cardNames) {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            for (var cardName : cardNames) {
                var row = db.jdbi().withHandle(h -> h.createQuery("""
                                SELECT name, oracle_text,
                                       face_1_name, face_1_oracle_text,
                                       face_2_name, face_2_oracle_text
                                FROM card
                                WHERE name = :name OR face_1_name = :name OR face_2_name = :name
                                LIMIT 1
                                """)
                        .bind("name", cardName)
                        .map((rs, ctx) -> new CardRow(
                                0L,
                                rs.getString("name"),
                                rs.getString("oracle_text"),
                                rs.getString("face_1_name"),
                                rs.getString("face_1_oracle_text"),
                                rs.getString("face_2_name"),
                                rs.getString("face_2_oracle_text")))
                        .findOne());

                if (row.isEmpty()) {
                    System.out.println(cardName + ": not found");
                    continue;
                }
                var card = row.get();
                try {
                    OracleParser.parse(card.name(), nullToEmpty(card.oracleText()));
                    if (card.face1Name() != null) {
                        OracleParser.parse(card.face1Name(), nullToEmpty(card.face1OracleText()));
                    }
                    if (card.face2Name() != null) {
                        OracleParser.parse(card.face2Name(), nullToEmpty(card.face2OracleText()));
                    }
                    System.out.println(card.name() + ": ok");
                } catch (Exception e) {
                    System.out.println(card.name() + ": fail: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
        }
    }

    private static boolean parseAndPrint(String label, String cardName, @Nullable String text) {
        System.out.println();
        System.out.println("── " + label + " ──");
        if (text == null || text.isBlank()) {
            System.out.println("(empty)");
            return true;
        }
        System.out.println(text);
        System.out.println();
        try {
            var abilities = OracleParser.parse(cardName, text);
            if (abilities.isEmpty()) {
                System.out.println("(no abilities parsed)");
            } else {
                for (var i = 0; i < abilities.size(); i++) {
                    System.out.println("[" + i + "] " + abilities.get(i));
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("FAILED: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private static List<CardRow> fetchAllVintage(Jdbi jdbi) {
        return jdbi.withHandle(h -> h.createQuery("SELECT card_id, name, oracle_text,"
                        + " face_1_name, face_1_oracle_text,"
                        + " face_2_name, face_2_oracle_text"
                        + " FROM card WHERE TRUE"
                        + VINTAGE_LEGAL_FILTER)
                .map((rs, ctx) -> new CardRow(
                        rs.getLong("card_id"),
                        rs.getString("name"),
                        rs.getString("oracle_text"),
                        rs.getString("face_1_name"),
                        rs.getString("face_1_oracle_text"),
                        rs.getString("face_2_name"),
                        rs.getString("face_2_oracle_text")))
                .list());
    }

    private static boolean tryParseCard(CardRow row) {
        try {
            OracleParser.parse(row.name(), nullToEmpty(row.oracleText()));
            if (row.face1Name() != null) {
                OracleParser.parse(row.face1Name(), nullToEmpty(row.face1OracleText()));
            }
            if (row.face2Name() != null) {
                OracleParser.parse(row.face2Name(), nullToEmpty(row.face2OracleText()));
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String nullToEmpty(@Nullable String s) {
        return s == null ? "" : s;
    }

    private static void markParsed(Jdbi jdbi, List<Long> ids) {
        batchSetOracleParsed(jdbi, ids, true);
    }

    private static void markUnparsed(Jdbi jdbi, List<Long> ids) {
        batchSetOracleParsed(jdbi, ids, false);
    }

    private static void batchSetOracleParsed(Jdbi jdbi, List<Long> ids, boolean value) {
        jdbi.useHandle(h -> {
            var batch = h.prepareBatch("UPDATE card SET oracle_parsed = :value WHERE card_id = :id");
            for (var id : ids) {
                batch.bind("id", id).bind("value", value).add();
            }
            batch.execute();
        });
    }

    private record CardRow(
            long cardId,
            String name,
            @Nullable String oracleText,
            @Nullable String face1Name,
            @Nullable String face1OracleText,
            @Nullable String face2Name,
            @Nullable String face2OracleText) {}

    private static void printHelp() {
        System.out.println("""
                MTG Oracle Parse Commands

                Usage:
                  mtg parse <subcommand>

                Subcommands:
                  all              Parse all cards with oracle_parsed = false.
                                   Marks cards whose oracle text parses without error.
                  <Card Name>      Parse a specific card by exact name (even if
                                   already parsed). Prints the parsed AST or the
                                   parser exception. Updates oracle_parsed only if
                                   every face parses.
                  reset            Set oracle_parsed = false for all cards.
                  status           Show how many cards are parsed.
                  unparsed [N]     Show the N shortest unparsed cards (default 20).
                  help             Show this help message.

                Examples:
                  mtg parse all
                  mtg parse "Lightning Bolt"
                  mtg parse reset
                """);
    }
}
