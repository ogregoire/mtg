package be.imgn.mtg.tooling.db;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/// Configuration for the tools module.
///
/// @param databasePath path to the H2 database file (without extension)
/// @param cacheDirectory directory for HTTP response caching
/// @param runtimeDirectory directory for runtime files like PID files
public record ToolsConfig(Path databasePath, Path cacheDirectory, Path runtimeDirectory) {

    private static final String APP_NAME = "mtg-engine";

    /// Creates a configuration with default platform-specific paths.
    ///
    /// Uses standard directories for each platform:
    /// - Linux: ~/.local/share/mtg-engine/ (data), ~/.cache/mtg-engine/ (cache)
    /// - macOS: ~/Library/Application Support/mtg-engine/ (data), ~/Library/Caches/mtg-engine/ (cache)
    /// - Windows: %LOCALAPPDATA%\mtg-engine\ (both)
    ///
    /// @return a ToolsConfig with default paths
    public static ToolsConfig withDefaults() {
        return withDefaults(FileSystems.getDefault());
    }

    /// Creates a configuration with default platform-specific paths using the given FileSystem.
    ///
    /// This overload is useful for testing with in-memory file systems like JimFS.
    ///
    /// @param fileSystem the file system to use for resolving paths
    /// @return a ToolsConfig with default paths
    public static ToolsConfig withDefaults(FileSystem fileSystem) {
        return withDefaults(fileSystem, System.getProperty("os.name"), System.getProperty("user.home"));
    }

    /// Creates a configuration with default paths for the given OS and home directory.
    ///
    /// This overload allows full control for testing.
    ///
    /// @param fileSystem the file system to use for resolving paths
    /// @param osName the operating system name (e.g., "Mac OS X", "Linux", "Windows 10")
    /// @param userHome the user's home directory path
    /// @return a ToolsConfig with default paths
    public static ToolsConfig withDefaults(FileSystem fileSystem, String osName, String userHome) {
        var home = fileSystem.getPath(userHome);
        var dataDir = getDataDirectory(fileSystem, osName, home);
        return new ToolsConfig(
                dataDir.resolve("cards"),
                getCacheDirectory(fileSystem, osName, home),
                getRuntimeDirectory(fileSystem, osName, dataDir));
    }

    private static Path getDataDirectory(FileSystem fileSystem, String osName, Path home) {
        var os = osName.toLowerCase();

        if (os.contains("mac")) {
            return home.resolve("Library/Application Support").resolve(APP_NAME);
        } else if (os.contains("win")) {
            var localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                return fileSystem.getPath(localAppData).resolve(APP_NAME);
            }
            return home.resolve("AppData/Local").resolve(APP_NAME);
        } else {
            // Linux/Unix - XDG Base Directory Specification
            var xdgDataHome = System.getenv("XDG_DATA_HOME");
            if (xdgDataHome != null) {
                return fileSystem.getPath(xdgDataHome).resolve(APP_NAME);
            }
            return home.resolve(".local/share").resolve(APP_NAME);
        }
    }

    private static Path getCacheDirectory(FileSystem fileSystem, String osName, Path home) {
        var os = osName.toLowerCase();

        if (os.contains("mac")) {
            return home.resolve("Library/Caches").resolve(APP_NAME);
        } else if (os.contains("win")) {
            var localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null) {
                return fileSystem.getPath(localAppData).resolve(APP_NAME).resolve("cache");
            }
            return home.resolve("AppData/Local").resolve(APP_NAME).resolve("cache");
        } else {
            // Linux/Unix - XDG Base Directory Specification
            var xdgCacheHome = System.getenv("XDG_CACHE_HOME");
            if (xdgCacheHome != null) {
                return fileSystem.getPath(xdgCacheHome).resolve(APP_NAME);
            }
            return home.resolve(".cache").resolve(APP_NAME);
        }
    }

    private static Path getRuntimeDirectory(FileSystem fileSystem, String osName, Path dataDir) {
        var os = osName.toLowerCase();

        if (os.contains("linux") || (os.contains("nix") || os.contains("nux"))) {
            // Linux/Unix - XDG Base Directory Specification
            // XDG_RUNTIME_DIR is typically /run/user/$UID and cleared on reboot
            var xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");
            if (xdgRuntimeDir != null) {
                return fileSystem.getPath(xdgRuntimeDir).resolve(APP_NAME);
            }
        }
        // macOS and Windows: use data directory (no standard runtime directory)
        return dataDir;
    }
}
