package be.imgn.mtg.engine.card.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.jdbi.v3.core.Jdbi;
import org.jspecify.annotations.Nullable;

import be.imgn.mtg.engine.ability.internal.parser.AbilityParser;
import be.imgn.mtg.engine.card.CardFetcher;
import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Value;
import be.imgn.mtg.engine.characteristics.internal.TypeLineParser;
import be.imgn.mtg.engine.game.Player;
import be.imgn.mtg.engine.mana.ManaCost;
import be.imgn.mtg.engine.object.Card;

/// Default implementation of [CardFetcher] backed by the H2 card database.
final class DefaultCardFetcher implements CardFetcher {

    private static final Map<Character, Color> COLOR_MAP = Map.of(
            'W', Color.WHITE,
            'U', Color.BLUE,
            'B', Color.BLACK,
            'R', Color.RED,
            'G', Color.GREEN);

    private static final String PID_FILE_NAME = "dbserver.pid";
    private static final int DEFAULT_TCP_PORT = 9092;

    private final Jdbi jdbi;

    DefaultCardFetcher() {
        this.jdbi = Jdbi.create(buildJdbcUrl(), "readonly", "readonly");
    }

    @Override
    public Optional<Card> fetchByName(String name, Player owner) {
        return jdbi.withHandle(handle -> handle.createQuery(
                        "SELECT * FROM card WHERE LOWER(name) = LOWER(:name) AND layout <> 'token' LIMIT 1")
                .bind("name", name)
                .mapToMap()
                .findFirst()
                .map(row -> buildCard(row, owner)));
    }

    @SuppressWarnings("NullAway") // row values are nullable per Map contract but name is always present
    private Card buildCard(Map<String, Object> row, Player owner) {
        var cardName = (String) row.get("name");
        var builder = Card.builder().name(cardName).owner(owner).controller(owner);

        // Type line
        var typeLine = (String) row.get("type_line");
        if (typeLine != null && !typeLine.isEmpty()) {
            var parsed = TypeLineParser.parse(typeLine);
            builder.types(parsed.types()).supertypes(parsed.supertypes()).subtypes(parsed.subtypes());
        }

        // Mana cost
        var manaCost = (String) row.get("mana_cost");
        if (manaCost != null && !manaCost.isEmpty()) {
            try {
                builder.manaCost(ManaCost.parse(manaCost));
            } catch (Exception _) {
                // Unparseable mana cost — skip
            }
        }

        // Power/toughness
        setPowerToughness(builder, (String) row.get("power"), (String) row.get("toughness"));

        // Loyalty
        var loyalty = (String) row.get("loyalty");
        if (loyalty != null && !loyalty.isEmpty()) {
            try {
                builder.loyalty(Value.of(Integer.parseInt(loyalty)));
            } catch (NumberFormatException _) {
                // Variable loyalty — skip
            }
        }

        // Colors
        var colors = (String) row.get("colors");
        if (colors != null) {
            for (int i = 0; i < colors.length(); i++) {
                var color = COLOR_MAP.get(colors.charAt(i));
                if (color != null) builder.addColor(color);
            }
        }

        // Oracle text → abilities + rules text
        var oracleText = (String) row.get("oracle_text");
        if (oracleText != null && !oracleText.isEmpty()) {
            builder.rulesText(oracleText);
            try {
                for (var ability : AbilityParser.parse(cardName, oracleText)) {
                    builder.addAbility(ability);
                }
            } catch (Exception _) {
                // Ability parsing failure — card still usable
            }
        }

        return builder.build();
    }

    private void setPowerToughness(Card.Builder builder, @Nullable String power, @Nullable String toughness) {
        if (power != null && !power.isEmpty()) {
            try {
                builder.power(Value.of(Integer.parseInt(power)));
            } catch (NumberFormatException _) {
                // Variable power (e.g., "*") — skip
            }
        }
        if (toughness != null && !toughness.isEmpty()) {
            try {
                builder.toughness(Value.of(Integer.parseInt(toughness)));
            } catch (NumberFormatException _) {
                // Variable toughness — skip
            }
        }
    }

    private static String buildJdbcUrl() {
        var dataDir = resolveDataDirectory();
        var dbPath = dataDir.resolve("cards");
        var pidFile = dataDir.resolve(PID_FILE_NAME);

        var serverPort = getServerPort(pidFile);
        if (serverPort != null) {
            return "jdbc:h2:tcp://localhost:" + serverPort + "/" + dbPath.toAbsolutePath();
        }
        return "jdbc:h2:file:" + dbPath + ";ACCESS_MODE_DATA=r";
    }

    private static @Nullable Integer getServerPort(Path pidFile) {
        if (!Files.exists(pidFile)) {
            return null;
        }
        try {
            var content = Files.readString(pidFile).trim();
            var parts = content.split(":", -1);
            var pid = Long.parseLong(parts[0]);
            var port = parts.length > 1 ? Integer.parseInt(parts[1]) : DEFAULT_TCP_PORT;
            if (ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                return port;
            }
            return null;
        } catch (IOException | NumberFormatException _) {
            return null;
        }
    }

    private static Path resolveDataDirectory() {
        var home = System.getProperty("user.home");
        var os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", "mtg-engine");
        } else if (os.contains("win")) {
            var localAppData = System.getenv("LOCALAPPDATA");
            return localAppData != null
                    ? Path.of(localAppData, "mtg-engine")
                    : Path.of(home, "AppData", "Local", "mtg-engine");
        } else {
            var xdgData = System.getenv("XDG_DATA_HOME");
            return xdgData != null ? Path.of(xdgData, "mtg-engine") : Path.of(home, ".local", "share", "mtg-engine");
        }
    }
}
