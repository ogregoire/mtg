package be.imgn.mtg.engine.ability.internal.parser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.util.Splitter;

/// Utility for generating parsers from oracle text word forms.
///
/// Looks up singular/plural forms from {@code oracle-words.properties} and generates
/// parsers that match both title-case and lowercase versions of each form.
public final class OracleParser {

    private static final Splitter COMMA = Splitter.on(',').trimResults();
    private static final Map<String, Parser<String>> CACHE = new ConcurrentHashMap<>();
    private static final Properties WORDS = loadWords();

    private OracleParser() {}

    /// Returns a parser matching any oracle text form of the given enum value.
    /// Looks up singular/plural forms from oracle-words.properties and matches
    /// both title-case and lowercase versions of each form.
    public static Parser<String> word(Enum<?> value) {
        var key = value.getDeclaringClass().getSimpleName() + "." + value.name();
        return CACHE.computeIfAbsent(key, OracleParser::buildFromKey);
    }

    /// Returns a parser matching any oracle text form of the given word.
    /// Looks up forms from oracle-words.properties by key (e.g., "permanent" -> "Permanents,Permanent").
    /// Matches both title-case and lowercase versions of each form.
    public static Parser<String> word(String key) {
        return CACHE.computeIfAbsent(key, OracleParser::buildFromKey);
    }

    private static Parser<String> buildFromKey(String key) {
        var forms = WORDS.getProperty(key);
        if (forms == null) {
            throw new IllegalArgumentException("No oracle words for: " + key);
        }
        return buildParser(forms);
    }

    private static Parser<String> buildParser(String forms) {
        return COMMA.split(forms)
                .flatMap(form -> {
                    var lower = form.toLowerCase(Locale.ROOT);
                    return lower.equals(form) ? Stream.of(form) : Stream.of(form, lower);
                })
                .distinct()
                .map(Parser::word)
                .collect(Parser.or());
    }

    private static Properties loadWords() {
        var props = new Properties();
        try (var stream = OracleParser.class.getResourceAsStream("/oracle-words.properties")) {
            if (stream == null) {
                throw new IllegalStateException("oracle-words.properties not found");
            }
            props.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return props;
    }
}
