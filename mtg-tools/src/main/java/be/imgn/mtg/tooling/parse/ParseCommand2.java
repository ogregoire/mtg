package be.imgn.mtg.tooling.parse;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.jdbi.v3.core.Jdbi;
import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.oracle2.parser.OracleParser;
import be.imgn.mtg.tooling.db.H2Database;
import be.imgn.mtg.tooling.db.ToolsConfig;

/// Oracle text parsing command for the oracle2 parser tree.
///
/// Mirrors [ParseCommand] but targets the `oracle_parsed2` column and
/// the [be.imgn.mtg.engine.oracle2.parser.OracleParser] entry point.
/// Queries the underlying `card` table (with a vintage-legality
/// filter) so the per-card "is parsed" decision can see all three
/// oracle_text fields at once — see [#parseFailure(CardRow)].
///
/// **A card is considered parsed** iff every non-blank
/// `oracle_text` / `face_1_oracle_text` / `face_2_oracle_text`
/// field parses. Vanilla cards (all fields null or blank) parse
/// vacuously. Blank face oracle_text is valid in vintage data —
/// it's how single-face cards (e.g., Grizzly Bears) and DFCs with
/// a vanilla face are represented.
///
/// Subcommands:
/// - `all` — reparses every vintage-legal card from scratch and
///   overwrites `oracle_parsed2` per the rule above. No `reset` is
///   required first; previous parse state is discarded.
/// - `<Card Name>` — parses one card by exact name, prints AST
/// - `reset` — sets `oracle_parsed2 = false` for all vintage cards
///   (clears state without reparsing)
public final class ParseCommand2 {

    private ParseCommand2() {}

    /// Restricts queries to cards that are currently legal or
    /// restricted in Vintage. Equivalent to the legality filter
    /// inside the `vintage` SQL view, but applied to the un-exploded
    /// `card` table so each row is one card.
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
            case "all" -> runAll(parseSetOnly(args.subList(1, args.size())));
            case "reset" -> runReset();
            case "status" -> runStatus(parseSetOnly(args.subList(1, args.size())));
            case "unparsed" -> runUnparsed(args.subList(1, args.size()));
            case "oracle" -> runOracle(args.subList(1, args.size()));
            case "-s", "--set" -> runAll(parseSetOnly(args));
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

    /// Restricts queries to cards with at least one PRINT in a CARD_SET
    /// matching the bound `:set` parameter (case-insensitive).
    private static String setFilter(@Nullable String set) {
        return set == null
                ? ""
                : " AND card_id IN (SELECT p.card_id FROM print p"
                        + " JOIN card_set cs ON p.set_id = cs.set_id"
                        + " WHERE UPPER(cs.code) = UPPER(:set) OR LOWER(cs.name) = LOWER(:set))";
    }

    private static void runUnparsed(List<String> args) {
        var opts = parseUnparsedArgs(args);
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var rows = db.jdbi().withHandle(h -> {
                var q = h.createQuery("SELECT name, LENGTH(oracle_text) AS len, oracle_text FROM vintage"
                                + " WHERE oracle_parsed2 = FALSE AND oracle_text IS NOT NULL"
                                + setFilter(opts.set())
                                + " ORDER BY LENGTH(oracle_text) ASC, name ASC LIMIT :limit")
                        .bind("limit", opts.limit());
                if (opts.set() != null) q = q.bind("set", opts.set());
                return q.map((rs, ctx) ->
                                new Object[] {rs.getString("name"), rs.getInt("len"), rs.getString("oracle_text")})
                        .list();
            });

            if (rows.isEmpty()) {
                System.out.println(
                        opts.set() == null ? "No unparsed cards." : "No unparsed cards in set " + opts.set() + ".");
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

    private record UnparsedOptions(@Nullable String set, int limit) {}

    private static UnparsedOptions parseUnparsedArgs(List<String> args) {
        String set = null;
        Integer limit = null;
        for (var i = 0; i < args.size(); i++) {
            var arg = args.get(i);
            switch (arg) {
                case "-s", "--set" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Missing value for " + arg);
                        System.exit(1);
                    }
                    set = args.get(++i);
                }
                default -> {
                    if (limit != null) {
                        System.err.println("Unexpected argument: " + arg);
                        System.exit(1);
                    }
                    limit = parsePositiveInt(arg);
                }
            }
        }
        return new UnparsedOptions(set, limit == null ? 20 : limit);
    }

    private static @Nullable String parseSetOnly(List<String> args) {
        String set = null;
        for (var i = 0; i < args.size(); i++) {
            var arg = args.get(i);
            if (arg.equals("-s") || arg.equals("--set")) {
                if (i + 1 >= args.size()) {
                    System.err.println("Missing value for " + arg);
                    System.exit(1);
                }
                set = args.get(++i);
            } else {
                System.err.println("Unexpected argument: " + arg);
                System.exit(1);
            }
        }
        return set;
    }

    private static void runStatus(@Nullable String set) {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var row = db.jdbi().withHandle(h -> {
                var q = h.createQuery("SELECT COUNT(*) AS total,"
                        + " SUM(CASE WHEN oracle_parsed2 THEN 1 ELSE 0 END) AS parsed"
                        + " FROM vintage WHERE TRUE"
                        + setFilter(set));
                if (set != null) q = q.bind("set", set);
                return q.map((rs, ctx) -> new long[] {rs.getLong("total"), rs.getLong("parsed")})
                        .one();
            });
            var total = row[0];
            var parsed = row[1];
            var remaining = total - parsed;
            var pct = total == 0 ? 0.0 : (100.0 * parsed) / total;
            if (set != null) {
                System.out.printf(Locale.ROOT, "Set:       %s%n", set);
            }
            System.out.printf(Locale.ROOT, "Parsed:    %d / %d (%.2f%%)%n", parsed, total, pct);
            System.out.printf(Locale.ROOT, "Remaining: %d%n", remaining);
        }
    }

    private static void runAll(@Nullable String set) {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var jdbi = db.jdbi();
            var cards = fetchAllVintage(jdbi, set);
            var totalFaces = cards.stream().mapToInt(ParseCommand2::faceCount).sum();
            System.out.println("Parsing " + totalFaces + " faces" + (set == null ? "..." : " in set " + set + "..."));

            var buildStart = System.nanoTime();
            OracleParser.parse("__warmup__", "Destroy target creature.");
            var buildEnd = System.nanoTime();
            System.out.println("Parser build: " + formatElapsed(buildEnd - buildStart));

            var start = System.nanoTime();
            var successFaces = 0;
            var failureFaces = 0;
            var regressions = new ArrayList<Regression>();
            var updates = new ArrayList<FlagUpdate>(256);
            for (var card : cards) {
                var oracle = parseField("oracle_text", card.name(), card.oracleText());
                var face1 = parseField(
                        "face_1_oracle_text",
                        Objects.requireNonNullElse(card.face1Name(), card.name()),
                        card.face1OracleText());
                var face2 = parseField(
                        "face_2_oracle_text",
                        Objects.requireNonNullElse(card.face2Name(), card.name()),
                        card.face2OracleText());

                // Stats: count per vintage-view face row.
                if (faceCount(card) == 1) {
                    if (oracle.parsed()) successFaces++;
                    else failureFaces++;
                } else {
                    if (card.face1Name() != null) {
                        if (face1.parsed()) successFaces++;
                        else failureFaces++;
                    }
                    if (card.face2Name() != null) {
                        if (face2.parsed()) successFaces++;
                        else failureFaces++;
                    }
                }

                // Regressions: per-field previously TRUE → now FALSE.
                if (card.oracleParsed() && !oracle.parsed() && oracle.failure() != null) {
                    regressions.add(new Regression(card.name(), oracle.failure()));
                }
                if (card.face1OracleParsed() && !face1.parsed() && face1.failure() != null) {
                    regressions.add(new Regression(card.name(), face1.failure()));
                }
                if (card.face2OracleParsed() && !face2.parsed() && face2.failure() != null) {
                    regressions.add(new Regression(card.name(), face2.failure()));
                }

                updates.add(new FlagUpdate(card.cardId(), oracle.parsed(), face1.parsed(), face2.parsed()));
                if (updates.size() >= 500) {
                    batchUpdateFlags(jdbi, updates);
                    updates.clear();
                }
            }
            if (!updates.isEmpty()) {
                batchUpdateFlags(jdbi, updates);
            }

            var total = successFaces + failureFaces;
            var pct = total == 0 ? 0.0 : (100.0 * successFaces) / total;
            System.out.printf(Locale.ROOT, "Success: %d, failures: %d (%.2f%%)%n", successFaces, failureFaces, pct);
            System.out.println("Done in " + formatElapsed(System.nanoTime() - start) + ".");
            if (!regressions.isEmpty()) {
                System.out.println();
                System.out.println("Regressions (" + regressions.size() + " face"
                        + (regressions.size() == 1 ? "" : "s")
                        + " previously parsed but no longer parse):");
                for (var r : regressions) {
                    System.out.println();
                    System.out.println("── " + r.name() + " (" + r.failure().face() + ") ──");
                    System.out.println(r.failure().oracleText());
                    System.out.println("FAILED: " + r.failure().error());
                }
            }
        }
    }

    /// Per-field parse outcome carrying the failure detail when
    /// applicable. `parsed = true` covers both real success and the
    /// vacuous null/blank case.
    private record FieldResult(boolean parsed, @Nullable ParseFailure failure) {}

    private static FieldResult parseField(String face, String name, @Nullable String text) {
        if (text == null || text.isBlank()) return new FieldResult(true, null);
        try {
            OracleParser.parse(name, text);
            return new FieldResult(true, null);
        } catch (Exception e) {
            return new FieldResult(
                    false, new ParseFailure(face, text, e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    /// How many face rows the `vintage` view emits for this card.
    /// Mirrors the view's three UNION arms: a card with no
    /// `face_*_name` set is single-face (1 row); otherwise count each
    /// non-null face name.
    private static int faceCount(CardRow c) {
        if (c.face1Name() == null && c.face2Name() == null) return 1;
        return (c.face1Name() != null ? 1 : 0) + (c.face2Name() != null ? 1 : 0);
    }

    private static String formatElapsed(long nanos) {
        var seconds = nanos / 1_000_000_000.0;
        if (seconds < 60) {
            return String.format(Locale.ROOT, "%.1f seconds", seconds);
        }
        var minutes = (int) (seconds / 60);
        var remainingSeconds = (int) Math.round(seconds - minutes * 60L);
        return String.format(Locale.ROOT, "%d:%02d minutes", minutes, remainingSeconds);
    }

    private static void runOracle(List<String> args) {
        if (args.isEmpty()) {
            System.err.println("Usage: mtg parse2 oracle \"<oracle text>\" [\"<oracle text>\" ...]");
            System.exit(1);
            return;
        }
        for (var i = 0; i < args.size(); i++) {
            if (i > 0) System.out.println();
            parseAndPrint("oracle[" + i + "]", "~", args.get(i));
        }
    }

    private static void runReset() {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var updated = db.jdbi().withHandle(h -> h.createUpdate("UPDATE card SET oracle_parsed2 = FALSE,"
                            + " face_1_oracle_parsed2 = FALSE, face_2_oracle_parsed2 = FALSE"
                            + " WHERE TRUE" + VINTAGE_LEGAL_FILTER)
                    .execute());
            System.out.println("Reset oracle_parsed2 / face_*_oracle_parsed2 on " + updated + " cards.");
        }
    }

    private static final String CARD_COLUMNS = "card_id, name, oracle_text, oracle_parsed2,"
            + " face_1_name, face_1_oracle_text, face_1_oracle_parsed2,"
            + " face_2_name, face_2_oracle_text, face_2_oracle_parsed2";

    private static final String SELECT_BY_NAME = "SELECT " + CARD_COLUMNS + " FROM card"
            + " WHERE name = :name OR face_1_name = :name OR face_2_name = :name"
            + " ORDER BY COALESCE(LENGTH(oracle_text), 0)"
            + "          + COALESCE(LENGTH(face_1_oracle_text), 0)"
            + "          + COALESCE(LENGTH(face_2_oracle_text), 0) DESC"
            + " LIMIT 1";

    private static CardRow mapCardRow(ResultSet rs) throws SQLException {
        return new CardRow(
                rs.getLong("card_id"),
                rs.getString("name"),
                rs.getString("oracle_text"),
                rs.getBoolean("oracle_parsed2"),
                rs.getString("face_1_name"),
                rs.getString("face_1_oracle_text"),
                rs.getBoolean("face_1_oracle_parsed2"),
                rs.getString("face_2_name"),
                rs.getString("face_2_oracle_text"),
                rs.getBoolean("face_2_oracle_parsed2"));
    }

    private static void runOne(String cardName) {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            var card = db.jdbi().withHandle(h -> h.createQuery(SELECT_BY_NAME)
                    .bind("name", cardName)
                    .map((rs, ctx) -> mapCardRow(rs))
                    .findOne());

            if (card.isEmpty()) {
                System.err.println("No card found with exact name: " + cardName);
                System.exit(1);
                return;
            }

            var c = card.get();
            System.out.println("Card: " + c.name());

            // Each non-blank field is parsed and shown; blanks print
            // "(empty)" via parseAndPrint and contribute true to that
            // field's flag.
            var oracleParsed = parseAndPrint("oracle_text", c.name(), c.oracleText());
            var face1Parsed = parseAndPrint(
                    "face_1_oracle_text", Objects.requireNonNullElse(c.face1Name(), c.name()), c.face1OracleText());
            var face2Parsed = parseAndPrint(
                    "face_2_oracle_text", Objects.requireNonNullElse(c.face2Name(), c.name()), c.face2OracleText());

            db.jdbi().useHandle(h -> h.createUpdate("UPDATE card SET oracle_parsed2 = :a,"
                            + " face_1_oracle_parsed2 = :b, face_2_oracle_parsed2 = :c"
                            + " WHERE card_id = :id")
                    .bind("id", c.cardId())
                    .bind("a", oracleParsed)
                    .bind("b", face1Parsed)
                    .bind("c", face2Parsed)
                    .execute());
        }
    }

    private static void runMany(List<String> cardNames) {
        try (var db = H2Database.create(ToolsConfig.withDefaults())) {
            for (var cardName : cardNames) {
                var card = db.jdbi().withHandle(h -> h.createQuery(SELECT_BY_NAME)
                        .bind("name", cardName)
                        .map((rs, ctx) -> mapCardRow(rs))
                        .findOne());

                if (card.isEmpty()) {
                    System.out.println(cardName + ": not found");
                    continue;
                }
                var c = card.get();
                var failed = parseFailure(c);
                if (failed == null) {
                    System.out.println(c.name() + ": ok");
                } else {
                    System.out.println(c.name() + ": fail (" + failed.face() + "): " + failed.error());
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

    private static List<CardRow> fetchAllVintage(Jdbi jdbi, @Nullable String set) {
        return jdbi.withHandle(h -> {
            var q = h.createQuery("SELECT " + CARD_COLUMNS
                    + " FROM card WHERE TRUE"
                    + VINTAGE_LEGAL_FILTER
                    + setFilter(set)
                    + " ORDER BY card_id");
            if (set != null) q = q.bind("set", set);
            return q.map((rs, ctx) -> mapCardRow(rs)).list();
        });
    }

    /// Returns null when every non-blank oracle_text field on the
    /// card parses; otherwise returns the first failure. Vanilla
    /// cards (no fields set, or all fields blank) parse vacuously.
    /// Both `null` and blank strings count as "no text to parse" —
    /// vintage data uses blank `face_*_oracle_text` to represent a
    /// face with no abilities (e.g., Grizzly Bears).
    private static @Nullable ParseFailure parseFailure(CardRow card) {
        var f = tryParseFace("oracle_text", card.name(), card.oracleText());
        if (f != null) return f;
        f = tryParseFace(
                "face_1_oracle_text",
                Objects.requireNonNullElse(card.face1Name(), card.name()),
                card.face1OracleText());
        if (f != null) return f;
        return tryParseFace(
                "face_2_oracle_text",
                Objects.requireNonNullElse(card.face2Name(), card.name()),
                card.face2OracleText());
    }

    /// Parse `text` as `name`'s oracle text. Returns null on success
    /// — including the "no text to parse" case (null or blank).
    /// Blank `face_*_oracle_text` is valid in vintage data: it's how
    /// vanilla single-face cards (and some DFC vanilla faces) are
    /// represented.
    private static @Nullable ParseFailure tryParseFace(String face, String name, @Nullable String text) {
        if (text == null || text.isBlank()) return null;
        try {
            OracleParser.parse(name, text);
            return null;
        } catch (Exception e) {
            return new ParseFailure(face, text, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private record ParseFailure(String face, String oracleText, String error) {}

    private record Regression(String name, ParseFailure failure) {}

    private record FlagUpdate(long cardId, boolean oracle, boolean face1, boolean face2) {}

    private static void batchUpdateFlags(Jdbi jdbi, List<FlagUpdate> updates) {
        jdbi.useHandle(h -> {
            var batch = h.prepareBatch("UPDATE card SET oracle_parsed2 = :a,"
                    + " face_1_oracle_parsed2 = :b, face_2_oracle_parsed2 = :c"
                    + " WHERE card_id = :id");
            for (var u : updates) {
                batch.bind("id", u.cardId())
                        .bind("a", u.oracle())
                        .bind("b", u.face1())
                        .bind("c", u.face2())
                        .add();
            }
            batch.execute();
        });
    }

    private record CardRow(
            long cardId,
            String name,
            @Nullable String oracleText,
            boolean oracleParsed,
            @Nullable String face1Name,
            @Nullable String face1OracleText,
            boolean face1OracleParsed,
            @Nullable String face2Name,
            @Nullable String face2OracleText,
            boolean face2OracleParsed) {}

    private static void printHelp() {
        System.out.println("""
                MTG Oracle2 Parse Commands

                A card is considered parsed iff every non-blank
                oracle_text / face_1_oracle_text / face_2_oracle_text
                field parses. Vanilla cards (all fields null or blank)
                parse vacuously. Blank face oracle_text is valid in
                vintage data (e.g., Grizzly Bears).

                Usage:
                  mtg parse2 <subcommand>

                Subcommands:
                  all [-s|--set <SET>]
                                   Reparse every vintage-legal card from scratch
                                   and overwrite oracle_parsed2. No `reset` is
                                   required first — previous parse state is
                                   discarded. Optional --set narrows to cards
                                   printed in SET.
                  <Card Name>      Parse a specific card by exact name. Prints the
                                   parsed AST or the parser exception. Updates
                                   oracle_parsed2 only if the card is parsed per
                                   the rule above.
                  reset            Set oracle_parsed2 = false for all vintage cards.
                  status [-s|--set <SET>]
                                   Show how many cards are parsed by oracle2.
                                   Optional --set narrows the totals.
                  unparsed [N] [-s|--set <SET>]
                                   Show the N shortest unparsed cards (default 20).
                                   Optional --set narrows to cards printed in SET.
                  oracle "<text>"  Parse free-form oracle text from the command line
                                   (no DB lookup). Use `~` to denote self-references.

                SET accepts either the set code (e.g., "M10", "ZEN") or full name
                (e.g., "Zendikar", "Magic 2010") — matched case-insensitively.

                Examples:
                  mtg parse2 all
                  mtg parse2 all --set ZEN
                  mtg parse2 status -s M10
                  mtg parse2 "Lightning Bolt"
                  mtg parse2 oracle "Destroy target creature."
                  mtg parse2 reset
                """);
    }
}
