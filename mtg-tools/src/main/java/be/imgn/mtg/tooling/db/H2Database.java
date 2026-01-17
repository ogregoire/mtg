package be.imgn.mtg.tooling.db;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jspecify.annotations.Nullable;

/// H2 database for MTG card data.
///
/// This implementation uses an embedded H2 database for persistent storage.
public final class H2Database implements AutoCloseable {

    private static final String PID_FILE_NAME = "dbserver.pid";
    private static final int DEFAULT_TCP_PORT = 9092;

    private final Jdbi jdbi;

    private H2Database(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /// Creates a new H2Database with the specified configuration.
    ///
    /// If the database server is running (detected via PID file), connects via TCP.
    /// Otherwise, connects directly to the file with AUTO_SERVER=TRUE.
    ///
    /// @param config the tools configuration
    /// @return a new H2Database instance
    public static H2Database create(ToolsConfig config) {
        var jdbcUrl = buildJdbcUrl(config);
        var jdbi = Jdbi.create(jdbcUrl);
        configureJdbi(jdbi);

        var db = new H2Database(jdbi);
        db.initializeSchema();

        return db;
    }

    /// Creates a new H2Database with the specified database path.
    ///
    /// @param dbPath path to the H2 database file (without extension)
    /// @return a new H2Database instance
    public static H2Database create(Path dbPath) {
        var jdbi = Jdbi.create("jdbc:h2:" + dbPath.toAbsolutePath() + ";AUTO_SERVER=TRUE");
        configureJdbi(jdbi);

        var db = new H2Database(jdbi);
        db.initializeSchema();
        return db;
    }

    private static String buildJdbcUrl(ToolsConfig config) {
        var pidFile = config.runtimeDirectory().resolve(PID_FILE_NAME);
        var serverInfo = getServerInfo(pidFile);

        if (serverInfo != null) {
            // Connect via TCP server using port from PID file
            return "jdbc:h2:tcp://localhost:" + serverInfo.port() + "/"
                    + config.databasePath().toAbsolutePath();
        }

        // Connect directly with AUTO_SERVER for concurrent access
        return "jdbc:h2:" + config.databasePath().toAbsolutePath() + ";AUTO_SERVER=TRUE";
    }

    /// Returns server info (pid and port) if server is running, null otherwise.
    private static @Nullable ServerInfo getServerInfo(Path pidFile) {
        if (!Files.exists(pidFile)) {
            return null;
        }

        try {
            var content = Files.readString(pidFile).trim();
            var parts = content.split(":");

            var pid = Long.parseLong(parts[0]);
            // Port is optional for backwards compatibility with old PID files
            var port = parts.length > 1 ? Integer.parseInt(parts[1]) : DEFAULT_TCP_PORT;

            // Check if process is still alive
            if (ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                return new ServerInfo(pid, port);
            }
            return null;
        } catch (IOException | NumberFormatException e) {
            return null;
        }
    }

    private record ServerInfo(long pid, int port) {}

    /// Creates a new in-memory H2Database for testing.
    ///
    /// @return a new in-memory H2Database instance
    public static H2Database createInMemory() {
        var jdbi = Jdbi.create("jdbc:h2:mem:testdb" + System.nanoTime() + ";DB_CLOSE_DELAY=-1");
        configureJdbi(jdbi);

        var db = new H2Database(jdbi);
        db.initializeSchema();
        return db;
    }

    private static void configureJdbi(Jdbi jdbi) {
        jdbi.installPlugin(new SqlObjectPlugin());
    }

    private void initializeSchema() {
        // Check if schema already exists by looking for the card table
        boolean schemaExists = jdbi.withHandle(handle -> {
            try {
                handle.createQuery("SELECT 1 FROM card LIMIT 1")
                        .mapTo(Integer.class)
                        .findOne();
                return true;
            } catch (Exception e) {
                return false;
            }
        });

        if (schemaExists) {
            return;
        }

        try (var stream = getClass().getResourceAsStream("/schema.sql")) {
            if (stream == null) {
                throw new IllegalStateException("schema.sql not found in classpath");
            }
            var schema = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            jdbi.useHandle(handle -> handle.createScript(schema).execute());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load schema", e);
        }
    }

    /// Gets the underlying JDBI instance for advanced operations.
    ///
    /// @return the JDBI instance
    public Jdbi jdbi() {
        return jdbi;
    }

    @Override
    public void close() {
        // H2 connections are managed by JDBI, no explicit close needed
    }
}
