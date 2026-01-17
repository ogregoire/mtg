package be.imgn.mtg.tooling.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.h2.tools.Server;

import be.imgn.mtg.tooling.Main;
import be.imgn.mtg.tooling.scryfall.ScryfallClient;
import be.imgn.mtg.tooling.scryfall.ScryfallSync;

/// Database management command.
///
/// Subcommands:
/// - `sync` - Sync card data from Scryfall
/// - `start` - Start the H2 database server
/// - `stop` - Stop the H2 database server
public final class DbCommand {

    private static final String DEFAULT_WEB_PORT = "8082";
    private static final String DEFAULT_TCP_PORT = "9092";

    private DbCommand() {}

    private static Path getBaseDir(ToolsConfig config) {
        var parent = config.databasePath().getParent();
        return parent != null ? parent : Path.of(".");
    }

    /// Runs the database command with the given arguments.
    ///
    /// @param args command-line arguments
    public static void run(List<String> args) {
        if (args.isEmpty()) {
            printHelp();
            System.exit(1);
        }

        var subcommand = args.getFirst();
        var remainingArgs = args.subList(1, args.size());

        switch (subcommand) {
            case "sync" -> runSync(remainingArgs);
            case "start" -> runStart(remainingArgs);
            case "stop" -> runStop(remainingArgs);
            case "clear" -> runClear(remainingArgs);
            case "-h", "--help", "help" -> printHelp();
            default -> {
                System.err.println("Unknown subcommand: " + subcommand);
                System.err.println("Run 'mtg db --help' for usage.");
                System.exit(1);
            }
        }
    }

    private static void runSync(List<String> args) {
        var config = parseSyncConfig(args);
        var mode = parseSyncMode(args);

        System.out.println("MTG Card Database Sync");
        System.out.println("Database: " + config.databasePath());
        System.out.println("Cache: " + config.cacheDirectory());
        System.out.println();

        // Ensure directories exist
        try {
            Files.createDirectories(config.databasePath().getParent());
            Files.createDirectories(config.cacheDirectory());
        } catch (IOException e) {
            System.err.println("Failed to create directories: " + e.getMessage());
            System.exit(1);
        }

        try (var db = H2Database.create(config);
                var client = new ScryfallClient(config.cacheDirectory())) {
            var sync = new ScryfallSync(client, db.jdbi());

            switch (mode) {
                case ALL -> sync.syncAll();
                case CARDS_ONLY -> {
                    System.out.println("Syncing cards from Scryfall...");
                    sync.syncCardsAndLegalities();
                }
                case RULINGS_ONLY -> {
                    System.out.println("Syncing rulings from Scryfall...");
                    sync.syncRulings();
                }
            }
        } catch (IOException e) {
            System.err.println("Sync failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runStart(List<String> args) {
        var foreground = args.contains("--fg") || args.contains("--foreground");
        var isDaemon = args.contains("--daemon");

        if (isDaemon) {
            // Internal: run server in foreground (called by spawned process)
            runServerForeground(args);
        } else if (foreground) {
            // User requested foreground mode
            runServerForeground(args);
        } else {
            // Default: spawn background process
            spawnBackgroundServer(args);
        }
    }

    private static void spawnBackgroundServer(List<String> args) {
        var config = parseServerConfig(args);
        var webPort = parseWebPort(args);
        var tcpPort = parseTcpPort(args);
        var pidFile = getPidFile(config);

        // Check if already running
        if (Files.exists(pidFile)) {
            System.err.println("Database server appears to be already running.");
            System.err.println("PID file exists: " + pidFile);
            System.err.println("Run 'mtg db stop' first if you want to restart.");
            System.exit(1);
        }

        try {
            // Ensure database directory exists
            var baseDir = getBaseDir(config);
            Files.createDirectories(baseDir);

            // Build command to spawn daemon process
            var javaHome = System.getProperty("java.home");
            var javaBin = Path.of(javaHome, "bin", "java").toString();
            var modulePath = System.getProperty("jdk.module.path");

            var command = new ArrayList<String>();
            command.add(javaBin);
            var mainClassName = Main.class.getName();
            var moduleName = Main.class.getModule().getName();
            if (modulePath != null && moduleName != null) {
                command.add("-p");
                command.add(modulePath);
                command.add("-m");
                command.add(moduleName + "/" + mainClassName);
            } else {
                // Fallback to classpath
                var classPath = System.getProperty("java.class.path");
                command.add("-cp");
                command.add(classPath);
                command.add(mainClassName);
            }
            command.add("db");
            command.add("start");
            command.add("--daemon");
            command.add("--db");
            command.add(config.databasePath().toString());
            command.add("--web-port");
            command.add(webPort);
            command.add("--tcp-port");
            command.add(tcpPort);

            var processBuilder = new ProcessBuilder(command);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);

            var process = processBuilder.start();
            var pid = process.pid();

            // Write PID file with format: pid:tcpPort
            Files.writeString(pidFile, pid + ":" + tcpPort);

            // Wait a moment for server to start
            Thread.sleep(1000);

            // Check if process is still alive
            if (!process.isAlive()) {
                Files.deleteIfExists(pidFile);
                System.err.println("Failed to start server (process exited immediately).");
                System.exit(1);
            }

            var webUrl = "http://localhost:" + webPort;
            var jdbcUrl = "jdbc:h2:tcp://localhost:" + tcpPort + "/"
                    + config.databasePath().toAbsolutePath();

            System.out.println("H2 server started successfully!");
            System.out.println("Web console: " + webUrl);
            System.out.println("JDBC URL: " + jdbcUrl);
            System.out.println("PID: " + pid);
            System.out.println();
            System.out.println("Run 'mtg db stop' to stop the server.");

            // Open browser
            Server.openBrowser(webUrl);

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            try {
                Files.deleteIfExists(pidFile);
            } catch (IOException ignored) {
                // Ignore
            }
            System.exit(1);
        }
    }

    private static void runServerForeground(List<String> args) {
        var config = parseServerConfig(args);
        var webPort = parseWebPort(args);
        var tcpPort = parseTcpPort(args);
        var tcpOnly = args.contains("--tcp-only");
        var pidFile = getPidFile(config);
        var isDaemon = args.contains("--daemon");

        // Only check PID file if not daemon (daemon is spawned after check)
        if (!isDaemon && Files.exists(pidFile)) {
            System.err.println("Database server appears to be already running.");
            System.err.println("PID file exists: " + pidFile);
            System.err.println("Run 'mtg db stop' first if you want to restart.");
            System.exit(1);
        }

        try {
            // Ensure database directory exists
            var baseDir = getBaseDir(config);
            Files.createDirectories(baseDir);

            // Start the TCP server for JDBC connections
            var tcpServer = Server.createTcpServer(
                    "-tcpPort", tcpPort, "-tcpAllowOthers", "-ifNotExists", "-baseDir", baseDir.toString());
            tcpServer.start();

            Server webServer = null;
            if (!tcpOnly) {
                // Start the web server (HTTP interface)
                webServer = Server.createWebServer(
                        "-webPort", webPort, "-webAllowOthers", "-ifNotExists", "-baseDir", baseDir.toString());
                webServer.start();
            }

            var webUrl = "http://localhost:" + webPort;
            var jdbcUrl = "jdbc:h2:tcp://localhost:" + tcpPort + "/"
                    + config.databasePath().toAbsolutePath();

            if (!isDaemon) {
                // Write PID file for foreground mode with format: pid:tcpPort
                Files.writeString(pidFile, ProcessHandle.current().pid() + ":" + tcpPort);

                System.out.println("H2 server started successfully!");
                if (!tcpOnly) {
                    System.out.println("Web console: " + webUrl);
                }
                System.out.println("JDBC URL: " + jdbcUrl);
                System.out.println();
                System.out.println("Press Ctrl+C to stop, or run 'mtg db stop' from another terminal.");

                // Open browser
                if (!tcpOnly) {
                    Server.openBrowser(webUrl);
                }
            }

            // Keep the server running
            var finalWebServer = webServer;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (!isDaemon) {
                    System.out.println("\nShutting down server...");
                }
                tcpServer.stop();
                if (finalWebServer != null) {
                    finalWebServer.stop();
                }
                try {
                    Files.deleteIfExists(pidFile);
                } catch (IOException e) {
                    // Ignore
                }
            }));

            // Block until interrupted
            Thread.currentThread().join();

        } catch (Exception e) {
            if (!isDaemon) {
                System.err.println("Failed to start server: " + e.getMessage());
            }
            try {
                Files.deleteIfExists(pidFile);
            } catch (IOException ignored) {
                // Ignore
            }
            System.exit(1);
        }
    }

    private static void runStop(List<String> args) {
        var config = parseServerConfig(args);
        var pidFile = getPidFile(config);

        if (!Files.exists(pidFile)) {
            System.err.println("Database server does not appear to be running.");
            System.err.println("No PID file found: " + pidFile);
            System.exit(1);
        }

        try {
            var content = Files.readString(pidFile).trim();
            if (content.isEmpty()) {
                System.err.println("Invalid PID file.");
                System.exit(1);
            }

            // Parse pid:port format (port is optional for backwards compatibility)
            long pid;
            var parts = content.split(":");
            pid = Long.parseLong(parts[0]);

            System.out.println("Stopping H2 database server (PID: " + pid + ")...");

            // Find and destroy the process
            var processHandle = ProcessHandle.of(pid);
            if (processHandle.isPresent()) {
                var process = processHandle.get();
                process.destroy();

                // Wait for process to terminate (up to 5 seconds)
                var terminated = process.onExit()
                        .orTimeout(5, TimeUnit.SECONDS)
                        .handle((p, ex) -> ex == null)
                        .join();

                if (!terminated) {
                    // Force kill if graceful shutdown didn't work
                    process.destroyForcibly();
                }
            }

            Files.deleteIfExists(pidFile);

            System.out.println("Server stopped successfully.");

        } catch (NumberFormatException e) {
            System.err.println("Invalid PID file format.");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Failed to stop server: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runClear(List<String> args) {
        var config = parseSyncConfig(args);
        var pidFile = getPidFile(config);

        // Check if server is running
        if (Files.exists(pidFile)) {
            System.err.println("Database server is currently running.");
            System.err.println("Please run 'mtg db stop' first.");
            System.exit(1);
        }

        // Delete database files (H2 creates .mv.db and possibly .trace.db files)
        var dbPath = config.databasePath();
        var dbDir = dbPath.getParent();
        var dbName = dbPath.getFileName().toString();
        var dbDeleted = false;

        if (dbDir != null && Files.exists(dbDir)) {
            try (var files = Files.list(dbDir)) {
                var dbFiles = files.filter(p -> {
                            var name = p.getFileName().toString();
                            return name.startsWith(dbName) && (name.endsWith(".mv.db") || name.endsWith(".trace.db"));
                        })
                        .toList();

                for (var file : dbFiles) {
                    Files.deleteIfExists(file);
                    dbDeleted = true;
                }
            } catch (IOException e) {
                System.err.println("Failed to delete database files: " + e.getMessage());
                System.exit(1);
            }
        }

        if (dbDeleted) {
            System.out.println("Database deleted.");
        } else {
            System.out.println("No database found.");
        }

        // Delete cache directory contents (but keep the directory itself)
        var cacheDir = config.cacheDirectory();
        var cacheDeleted = false;

        if (Files.exists(cacheDir)) {
            try (var entries = Files.list(cacheDir)) {
                var cacheFiles = entries.toList();
                for (var file : cacheFiles) {
                    deleteRecursively(file);
                    cacheDeleted = true;
                }
            } catch (IOException e) {
                System.err.println("Failed to delete cache contents: " + e.getMessage());
                System.exit(1);
            }
        }

        if (cacheDeleted) {
            System.out.println("Cache cleared.");
        } else {
            System.out.println("No cache found.");
        }
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (var entry : entries.toList()) {
                    deleteRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }

    private static ToolsConfig parseSyncConfig(List<String> args) {
        var defaults = ToolsConfig.withDefaults();
        var dbPath = defaults.databasePath();
        var cachePath = defaults.cacheDirectory();

        for (int i = 0; i < args.size(); i++) {
            if ("--db".equals(args.get(i)) && i + 1 < args.size()) {
                dbPath = Path.of(args.get(++i));
            } else if ("--cache".equals(args.get(i)) && i + 1 < args.size()) {
                cachePath = Path.of(args.get(++i));
            }
        }

        return new ToolsConfig(dbPath, cachePath, defaults.runtimeDirectory());
    }

    private static ToolsConfig parseServerConfig(List<String> args) {
        var defaults = ToolsConfig.withDefaults();
        var dbPath = defaults.databasePath();

        for (int i = 0; i < args.size(); i++) {
            if ("--db".equals(args.get(i)) && i + 1 < args.size()) {
                dbPath = Path.of(args.get(++i));
            }
        }

        return new ToolsConfig(dbPath, defaults.cacheDirectory(), defaults.runtimeDirectory());
    }

    private static String parseWebPort(List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            if (("--port".equals(args.get(i)) || "--web-port".equals(args.get(i))) && i + 1 < args.size()) {
                return args.get(++i);
            }
        }
        return DEFAULT_WEB_PORT;
    }

    private static String parseTcpPort(List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            if ("--tcp-port".equals(args.get(i)) && i + 1 < args.size()) {
                return args.get(++i);
            }
        }
        return DEFAULT_TCP_PORT;
    }

    private static SyncMode parseSyncMode(List<String> args) {
        for (var arg : args) {
            if ("--cards-only".equals(arg)) {
                return SyncMode.CARDS_ONLY;
            } else if ("--rulings-only".equals(arg)) {
                return SyncMode.RULINGS_ONLY;
            }
        }
        return SyncMode.ALL;
    }

    private static Path getPidFile(ToolsConfig config) {
        return config.runtimeDirectory().resolve("dbserver.pid");
    }

    private static void printHelp() {
        System.out.println("""
                MTG Database Commands

                Usage:
                  mtg db <subcommand> [options]

                Subcommands:
                  sync        Sync card data from Scryfall
                  start       Start the H2 database server
                  stop        Stop the H2 database server
                  clear       Delete the database and cache

                Sync Options:
                  --db <path>      Path to the database file (platform-specific default)
                  --cache <path>   Path to the cache directory (platform-specific default)
                  --cards-only     Sync only card data
                  --rulings-only   Sync only rulings data

                Server Options:
                  --db <path>        Path to the database file (platform-specific default)
                  --web-port <port>  HTTP port for H2 web console (default: 8082)
                  --tcp-port <port>  TCP port for JDBC connections (default: 9092)
                  --tcp-only         Start only TCP server (skip web console)
                  --fg               Run server in foreground (default: background)

                Clear Options:
                  --db <path>      Path to the database file (platform-specific default)
                  --cache <path>   Path to the cache directory (platform-specific default)

                Examples:
                  mtg db sync                     Sync all data with default paths
                  mtg db sync --db /path/to/db    Sync to custom database location
                  mtg db sync --cards-only        Sync only cards
                  mtg db start                    Start H2 server in background
                  mtg db start --fg               Start H2 server in foreground
                  mtg db start --web-port 9000    Start with custom web console port
                  mtg db stop                     Stop running H2 server
                  mtg db clear                    Delete database and cache files
                """);
    }

    private enum SyncMode {
        ALL,
        CARDS_ONLY,
        RULINGS_ONLY
    }
}
