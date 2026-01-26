package be.imgn.mtg.tooling.card;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import be.imgn.mtg.tooling.card.model.CardResult;
import be.imgn.mtg.tooling.db.H2Database;
import be.imgn.mtg.tooling.db.ToolsConfig;
import be.imgn.mtg.tooling.db.dao.CardQueryDao;

/// Card search and display command.
///
/// Usage:
/// ```
/// mtg card [options]
/// ```
///
/// Options:
/// - `--name <pattern>` - Card name (default: `*`, use `*` for wildcards)
/// - `--colors <colors>` - Filter by card color (comma-separated: red, blue, black, white, green)
/// - `--color-id <colors>` - Filter by color identity (comma-separated)
/// - `--type <type>` - Filter by type line (card types/supertypes validated, subtypes use `*`)
/// - `--oracle <pattern>` - Filter by oracle text (use `*` for wildcards)
/// - `--set <pattern>` - Filter by set name (use `*` for wildcards)
/// - `--format <name>` - Filter by format legality
/// - `--dfc [yes|no]` - Filter double-faced cards (`--dfc` or `--dfc yes`: only DFC, `--dfc no`: exclude DFC)
/// - `--page <n>` - Page number (default: 1)
public final class CardCommand {

    private static final int PAGE_SIZE = 20;
    private static final Set<String> VALID_COLORS =
            Set.of("white", "blue", "black", "red", "green", "w", "u", "b", "r", "g");

    /// Valid card types (case-insensitive).
    private static final Set<String> VALID_CARD_TYPES = Set.of(
            "artifact",
            "battle",
            "creature",
            "enchantment",
            "instant",
            "land",
            "planeswalker",
            "sorcery",
            "kindred",
            "tribal",
            "dungeon",
            "conspiracy",
            "phenomenon",
            "plane",
            "scheme",
            "vanguard");

    /// Valid supertypes (case-insensitive).
    private static final Set<String> VALID_SUPERTYPES =
            Set.of("basic", "legendary", "ongoing", "snow", "world", "host", "elite");

    private CardCommand() {}

    /// Runs the card command with the given arguments.
    ///
    /// @param args command-line arguments
    public static void run(List<String> args) {
        if (args.isEmpty() || args.contains("-h") || args.contains("--help") || args.contains("help")) {
            printHelp();
            return;
        }

        var options = parseOptions(args);
        if (options == null) {
            System.exit(1);
            return;
        }

        // Default name pattern to "*" (all cards) if not provided
        var name = options.name != null ? options.name : "*";

        var config = ToolsConfig.withDefaults();
        try (var db = H2Database.create(config)) {
            var dao = new CardQueryDao(db.jdbi());
            executeSearch(dao, name, options, buildOriginalCommand(args));
        }
    }

    private static void executeSearch(CardQueryDao dao, String name, Options options, String originalCommand) {
        var namePattern = wildcardToSql(name);

        // First, try exact match if no wildcards
        if (!name.contains("*")) {
            var exactMatch = dao.findByExactName(name);
            if (exactMatch.isPresent()) {
                displaySingleCard(dao, exactMatch.get());
                return;
            }
        }

        // Execute search based on filters
        List<CardResult> results;
        int totalCount;

        var offset = (options.page - 1) * PAGE_SIZE;

        if (options.format != null) {
            // Format filter takes precedence
            results = dao.searchByFormat(namePattern, options.format, PAGE_SIZE, offset);
            totalCount = dao.countByFormat(namePattern, options.format);
        } else if (options.set != null) {
            // Set filter
            var setPattern = wildcardToSql(options.set);
            results = dao.searchBySet(namePattern, setPattern, PAGE_SIZE, offset);
            totalCount = dao.countBySet(namePattern, setPattern);
        } else {
            // General search with optional type, oracle, color, color identity, and DFC filters
            var typeCondition = buildTypeCondition(options.types);
            var oraclePattern = options.oracle != null ? wildcardToSql(options.oracle) : null;
            var colorCondition = buildColorCondition(options.colors);
            var colorIdentityCondition = buildColorIdentityCondition(options.colorIdentity);
            var dfcCondition = buildDfcCondition(options.dfc);

            results = dao.searchCards(
                    namePattern,
                    typeCondition,
                    oraclePattern,
                    colorCondition,
                    colorIdentityCondition,
                    dfcCondition,
                    PAGE_SIZE,
                    offset);
            totalCount = dao.countCards(
                    namePattern, typeCondition, oraclePattern, colorCondition, colorIdentityCondition, dfcCondition);
        }

        if (results.isEmpty()) {
            System.out.println("No cards found matching your criteria.");
            return;
        }

        // Single result -> display full details
        if (totalCount == 1) {
            displaySingleCard(dao, results.getFirst());
            return;
        }

        // Multiple results -> display list
        System.out.print(
                CardFormatter.formatSearchResults(results, totalCount, options.page, PAGE_SIZE, originalCommand));
    }

    private static void displaySingleCard(CardQueryDao dao, CardResult card) {
        var legalities = dao.getLegalities(card.cardId());
        var prints = dao.getPrints(card.cardId());
        var totalSets = dao.countSets(card.cardId());
        var rulings = dao.getRulings(card.cardId());
        var mostRecentPrint = dao.getMostRecentPrint(card.cardId()).orElse(null);

        System.out.print(CardFormatter.formatCard(card, legalities, prints, totalSets, rulings, mostRecentPrint));
    }

    private static String wildcardToSql(String pattern) {
        // Convert user-friendly * wildcards to SQL % wildcards
        // Also escape any existing % or _ characters
        return pattern.replace("%", "\\%").replace("_", "\\_").replace("*", "%");
    }

    private static @Nullable String buildColorCondition(@Nullable List<String> colors) {
        if (colors == null || colors.isEmpty()) {
            return null;
        }

        var conditions = new ArrayList<String>();
        for (var color : colors) {
            var colorCode = normalizeColorCode(color);
            if (colorCode != null) {
                conditions.add("colors LIKE '%" + colorCode + "%'");
            }
        }

        return conditions.isEmpty() ? null : String.join(" AND ", conditions);
    }

    private static @Nullable String buildColorIdentityCondition(@Nullable List<String> colorIdentity) {
        if (colorIdentity == null || colorIdentity.isEmpty()) {
            return null;
        }

        var conditions = new ArrayList<String>();
        for (var color : colorIdentity) {
            var colorCode = normalizeColorCode(color);
            if (colorCode != null) {
                conditions.add("color_identity LIKE '%" + colorCode + "%'");
            }
        }

        return conditions.isEmpty() ? null : String.join(" AND ", conditions);
    }

    private static @Nullable String buildTypeCondition(@Nullable List<String> types) {
        if (types == null || types.isEmpty()) {
            return null;
        }

        var conditions = new ArrayList<String>();
        for (var type : types) {
            // Convert * wildcards to SQL % wildcards
            var sqlPattern = type.replace("%", "\\%").replace("_", "\\_").replace("*", "%");
            conditions.add("LOWER(type_line) LIKE LOWER('" + sqlPattern + "')");
        }

        return conditions.isEmpty() ? null : String.join(" AND ", conditions);
    }

    private static @Nullable String buildDfcCondition(@Nullable Boolean dfc) {
        if (dfc == null) {
            return null;
        }
        // DFC cards have both face_1_name and face_2_name not null
        if (dfc) {
            return "face_1_name IS NOT NULL AND face_2_name IS NOT NULL";
        } else {
            return "face_1_name IS NULL OR face_2_name IS NULL";
        }
    }

    private static @Nullable String normalizeColorCode(String color) {
        return switch (color.toLowerCase(Locale.ROOT)) {
            case "white", "w" -> "W";
            case "blue", "u" -> "U";
            case "black", "b" -> "B";
            case "red", "r" -> "R";
            case "green", "g" -> "G";
            default -> null;
        };
    }

    private static @Nullable List<String> validateAndNormalizeTypes(String typeArg) {
        var types = new ArrayList<String>();

        for (var type : typeArg.split(",", -1)) {
            var trimmed = type.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // If it contains wildcards, accept as-is (user knows what they're doing)
            if (trimmed.contains("*")) {
                types.add(trimmed);
                continue;
            }

            // Check if it's a valid card type or supertype (case-insensitive)
            var lowerType = trimmed.toLowerCase(Locale.ROOT);
            if (VALID_CARD_TYPES.contains(lowerType) || VALID_SUPERTYPES.contains(lowerType)) {
                // Auto-wrap with wildcards for searching
                types.add("*" + trimmed + "*");
            } else {
                // Not a valid type and no wildcards - show error
                System.err.println("Error: '" + trimmed + "' is not a valid card type or supertype.");
                System.err.println("Valid types: "
                        + String.join(", ", VALID_CARD_TYPES.stream().sorted().toList()));
                System.err.println("Valid supertypes: "
                        + String.join(", ", VALID_SUPERTYPES.stream().sorted().toList()));
                System.err.println("For subtypes (e.g., Human, Goblin), use wildcards: --type \"*Human*\"");
                return null;
            }
        }

        return types.isEmpty() ? null : types;
    }

    private static String buildOriginalCommand(List<String> args) {
        var sb = new StringBuilder("./mtg card");
        for (var arg : args) {
            sb.append(" ");
            // Quote arguments containing spaces or wildcards to prevent shell expansion
            if (arg.contains(" ") || arg.contains("*")) {
                sb.append("\"").append(arg).append("\"");
            } else {
                sb.append(arg);
            }
        }
        return sb.toString();
    }

    private static @Nullable Options parseOptions(List<String> args) {
        var options = new Options();

        for (var i = 0; i < args.size(); i++) {
            var arg = args.get(i);
            switch (arg) {
                case "--name", "-n" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --name requires a value");
                        return null;
                    }
                    options.name = args.get(++i);
                }
                case "--colors", "--color", "-c" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --colors requires a value");
                        return null;
                    }
                    options.colors = parseColors(args.get(++i));
                    if (options.colors == null) {
                        return null;
                    }
                }
                case "--color-id", "--color-identity" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --color-id requires a value");
                        return null;
                    }
                    options.colorIdentity = parseColors(args.get(++i));
                    if (options.colorIdentity == null) {
                        return null;
                    }
                }
                case "--type", "-t" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --type requires a value");
                        return null;
                    }
                    var typeArg = args.get(++i);
                    options.types = validateAndNormalizeTypes(typeArg);
                    if (options.types == null) {
                        return null;
                    }
                }
                case "--oracle", "-o" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --oracle requires a value");
                        return null;
                    }
                    options.oracle = args.get(++i);
                }
                case "--set", "-s" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --set requires a value");
                        return null;
                    }
                    options.set = args.get(++i);
                }
                case "--format", "-f" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --format requires a value");
                        return null;
                    }
                    options.format = args.get(++i);
                }
                case "--page", "-p" -> {
                    if (i + 1 >= args.size()) {
                        System.err.println("Error: --page requires a value");
                        return null;
                    }
                    try {
                        options.page = Integer.parseInt(args.get(++i));
                        if (options.page < 1) {
                            System.err.println("Error: --page must be at least 1");
                            return null;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Error: --page must be a number");
                        return null;
                    }
                }
                case "--dfc" -> {
                    // Check if next argument is yes/no, otherwise default to yes
                    if (i + 1 < args.size()) {
                        var nextArg = args.get(i + 1).toLowerCase(Locale.ROOT);
                        if ("yes".equals(nextArg) || "true".equals(nextArg)) {
                            options.dfc = true;
                            i++;
                        } else if ("no".equals(nextArg) || "false".equals(nextArg)) {
                            options.dfc = false;
                            i++;
                        } else {
                            // Next arg is not yes/no, so --dfc means yes
                            options.dfc = true;
                        }
                    } else {
                        options.dfc = true;
                    }
                }
                default -> {
                    if (arg.startsWith("-")) {
                        System.err.println("Unknown option: " + arg);
                        System.err.println("Run 'mtg card --help' for usage.");
                        return null;
                    }
                    // Positional argument - treat as name if not set
                    if (options.name == null) {
                        options.name = arg;
                    } else {
                        System.err.println("Unexpected argument: " + arg);
                        return null;
                    }
                }
            }
        }

        return options;
    }

    private static @Nullable List<String> parseColors(String colorStr) {
        var colors = new ArrayList<String>();
        for (var color : colorStr.split(",", -1)) {
            var trimmed = color.trim().toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                if (!VALID_COLORS.contains(trimmed)) {
                    System.err.println("Invalid color: " + trimmed);
                    System.err.println("Valid colors: white, blue, black, red, green (or w, u, b, r, g)");
                    return null;
                }
                colors.add(trimmed);
            }
        }
        return colors;
    }

    private static void printHelp() {
        System.out.println("""
                MTG Card Search

                Usage:
                  mtg card [options]

                Options:
                  --name, -n <pattern>    Card name to search for (default: *)
                                          Use * for wildcards (e.g., "*Bolt*")
                  --colors, --color, -c   Filter by card color (comma-separated)
                                          Valid: white, blue, black, red, green
                                          Or use: w, u, b, r, g
                  --color-id <colors>     Filter by color identity (comma-separated)
                                          Same values as --colors
                  --type, -t <type>       Filter by type line
                                          Card types: creature, instant, sorcery, etc.
                                          Supertypes: legendary, basic, snow, etc.
                                          Subtypes: use wildcards (e.g., "*Human*")
                  --oracle, -o <pattern>  Filter by oracle text (use * for wildcards)
                  --set, -s <pattern>     Filter by set name (use * for wildcards)
                  --format, -f <name>     Filter by format legality
                  --dfc [yes|no]          Filter double-faced cards (default: yes if specified)
                                          --dfc or --dfc yes: only DFC cards
                                          --dfc no: exclude DFC cards
                  --page, -p <n>          Page number (default: 1)

                Examples:
                  mtg card --name "Lightning Bolt"
                      Show details for Lightning Bolt

                  mtg card --name "*Bolt*"
                      Search for cards with "Bolt" in the name

                  mtg card --colors red,blue
                      Search for red and blue cards (actual card color)

                  mtg card --color-id w,u,b,r,g
                      Search for 5-color identity cards (e.g., Bringer cycle)

                  mtg card --name "*Dragon*" --format modern
                      Search for Dragons legal in Modern

                  mtg card --set "Zendikar*"
                      Search for cards from Zendikar sets

                  mtg card --type creature
                      Search for all creatures

                  mtg card --type legendary,creature
                      Search for legendary creatures

                  mtg card --type "*Human*"
                      Search for Human subtype (use wildcards for subtypes)

                  mtg card --type creature --oracle "draw a card"
                      Search for creatures with "draw a card" text

                  mtg card --dfc
                      Search for all double-faced cards

                  mtg card --type "Creature" --dfc no
                      Search for single-faced creatures

                  mtg card --name "*Bolt*" --page 2
                      Show page 2 of Bolt search results
                """);
    }

    private static class Options {
        @Nullable
        String name;

        @Nullable
        List<String> colors;

        @Nullable
        List<String> colorIdentity;

        @Nullable
        List<String> types;

        @Nullable
        String oracle;

        @Nullable
        String set;

        @Nullable
        String format;

        @Nullable
        Boolean dfc;

        int page = 1;
    }
}
