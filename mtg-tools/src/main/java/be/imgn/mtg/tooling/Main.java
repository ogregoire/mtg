package be.imgn.mtg.tooling;

import java.util.List;

import be.imgn.mtg.tooling.card.CardCommand;
import be.imgn.mtg.tooling.db.DbCommand;
import be.imgn.mtg.tooling.parse.ParseCommand;
import be.imgn.mtg.tooling.rules.RulesCommand;
import be.imgn.mtg.tooling.sql.SqlCommand;

/// Main entry point for the MTG command-line interface.
///
/// Usage:
/// ```
/// mtg <command> [subcommand] [options]
/// ```
///
/// Commands:
/// - `db` - Database management commands (sync, start, stop)
public final class Main {

    private Main() {}

    /// Main entry point.
    ///
    /// @param args command-line arguments
    public static void main(String[] args) {
        run(List.of(args));
    }

    private static void run(List<String> args) {
        if (args.isEmpty()) {
            printHelp();
            System.exit(1);
        }

        var command = args.getFirst();
        var remainingArgs = args.subList(1, args.size());

        switch (command) {
            case "card" -> CardCommand.run(remainingArgs);
            case "db" -> DbCommand.run(remainingArgs);
            case "parse" -> ParseCommand.run(remainingArgs);
            case "rules" -> RulesCommand.run(remainingArgs);
            case "sql" -> SqlCommand.run(remainingArgs);
            case "-h", "--help", "help" -> printHelp();
            case "-v", "--version", "version" -> printVersion();
            default -> {
                System.err.println("Unknown command: " + command);
                System.err.println("Run 'mtg --help' for usage.");
                System.exit(1);
            }
        }
    }

    private static void printHelp() {
        System.out.println("""
                MTG Engine CLI

                Usage:
                  mtg <command> [subcommand] [options]

                Commands:
                  card        Search and display card information
                  db          Database management (sync, start, stop)
                  parse       Parse oracle text via the oracle parser
                  rules       Look up MTG Comprehensive Rules
                  sql         Run read-only SQL queries on the database
                  help        Show this help message
                  version     Show version information

                Examples:
                  mtg card --name "Lightning Bolt"
                      Show card details for Lightning Bolt

                  mtg card --name "*Bolt*"
                      Search for cards with "Bolt" in the name

                  mtg db sync           Sync card data from Scryfall
                  mtg db start          Start the H2 database server
                  mtg db stop           Stop the H2 database server

                  mtg rules flying      Show rules for Flying keyword
                  mtg rules 702.9       Show rule 702.9 and sub-rules
                  mtg rules --search "damage"
                                        Search rules for "damage"
                  mtg rules --glossary "mana value"
                                        Look up glossary term

                  mtg sql "SELECT name, mana_cost FROM card LIMIT 5"
                                        Run a SQL query

                Run 'mtg <command> --help' for more information on a command.
                """);
    }

    private static void printVersion() {
        System.out.println("MTG Engine CLI v0.1.0-SNAPSHOT");
    }
}
