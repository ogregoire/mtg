package be.imgn.mtg.tooling.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.FileSystem;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToolsConfigTest {

    private FileSystem macFs;
    private FileSystem linuxFs;
    private FileSystem windowsFs;

    @BeforeEach
    void setUp() {
        macFs = Jimfs.newFileSystem(Configuration.osX());
        linuxFs = Jimfs.newFileSystem(Configuration.unix());
        windowsFs = Jimfs.newFileSystem(Configuration.windows());
    }

    @AfterEach
    void tearDown() throws IOException {
        macFs.close();
        linuxFs.close();
        windowsFs.close();
    }

    @Test
    void macOsUsesLibraryPaths() {
        var config = ToolsConfig.withDefaults(macFs, "Mac OS X", "/Users/testuser", _ -> null);

        assertThat(config.databasePath().toString())
                .isEqualTo("/Users/testuser/Library/Application Support/mtg-engine/cards");
        assertThat(config.cacheDirectory().toString()).isEqualTo("/Users/testuser/Library/Caches/mtg-engine");
        assertThat(config.runtimeDirectory().toString())
                .isEqualTo("/Users/testuser/Library/Application Support/mtg-engine");
    }

    @Test
    void linuxUsesXdgPaths() {
        var config = ToolsConfig.withDefaults(linuxFs, "Linux", "/home/testuser", _ -> null);

        assertThat(config.databasePath().toString()).isEqualTo("/home/testuser/.local/share/mtg-engine/cards");
        assertThat(config.cacheDirectory().toString()).isEqualTo("/home/testuser/.cache/mtg-engine");
        // Runtime directory falls back to data directory when XDG_RUNTIME_DIR is not set
        assertThat(config.runtimeDirectory().toString()).isEqualTo("/home/testuser/.local/share/mtg-engine");
    }

    @Test
    void windowsUsesAppDataPaths() {
        var config = ToolsConfig.withDefaults(windowsFs, "Windows 10", "C:\\Users\\testuser", _ -> null);

        // When LOCALAPPDATA is not set, falls back to AppData/Local
        assertThat(config.databasePath().toString())
                .isEqualTo("C:\\Users\\testuser\\AppData\\Local\\mtg-engine\\cards");
        assertThat(config.cacheDirectory().toString())
                .isEqualTo("C:\\Users\\testuser\\AppData\\Local\\mtg-engine\\cache");
        assertThat(config.runtimeDirectory().toString()).isEqualTo("C:\\Users\\testuser\\AppData\\Local\\mtg-engine");
    }

    @Test
    void databasePathEndsWithCards() {
        var config = ToolsConfig.withDefaults(linuxFs, "Linux", "/home/testuser", _ -> null);

        assertThat(config.databasePath().getFileName().toString()).isEqualTo("cards");
    }

    @Test
    void allPathsAreAbsolute() {
        var config = ToolsConfig.withDefaults(linuxFs, "Linux", "/home/testuser", _ -> null);

        assertThat(config.databasePath().isAbsolute()).isTrue();
        assertThat(config.cacheDirectory().isAbsolute()).isTrue();
        assertThat(config.runtimeDirectory().isAbsolute()).isTrue();
    }

    @Test
    void recordComponentsMatchConstructorOrder() {
        var dbPath = linuxFs.getPath("/custom/db");
        var cachePath = linuxFs.getPath("/custom/cache");
        var runtimePath = linuxFs.getPath("/custom/runtime");

        var config = new ToolsConfig(dbPath, cachePath, runtimePath);

        assertThat(config.databasePath()).isEqualTo(dbPath);
        assertThat(config.cacheDirectory()).isEqualTo(cachePath);
        assertThat(config.runtimeDirectory()).isEqualTo(runtimePath);
    }
}
