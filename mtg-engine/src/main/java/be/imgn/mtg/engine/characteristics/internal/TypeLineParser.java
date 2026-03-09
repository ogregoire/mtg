package be.imgn.mtg.engine.characteristics.internal;

import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.caseInsensitive;
import static com.google.common.labs.parse.Parser.caseInsensitiveWord;
import static com.google.common.labs.parse.Parser.or;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

import be.imgn.mtg.engine.characteristics.ArtifactType;
import be.imgn.mtg.engine.characteristics.BasicLandType;
import be.imgn.mtg.engine.characteristics.CreatureType;
import be.imgn.mtg.engine.characteristics.EnchantmentType;
import be.imgn.mtg.engine.characteristics.NonBasicLandType;
import be.imgn.mtg.engine.characteristics.PlaneswalkerType;
import be.imgn.mtg.engine.characteristics.SpellType;
import be.imgn.mtg.engine.characteristics.Subtype;
import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.characteristics.Types;

/// Parses MTG type lines into supertypes, types, and subtypes.
///
/// Type lines follow the pattern: `[Supertypes] Types [— Subtypes]`
public final class TypeLineParser {

    private TypeLineParser() {}

    private static final CharPredicate WHITESPACE = CharPredicate.is(' ');

    private static final Parser<Supertype> SUPERTYPE = Arrays.stream(Supertype.values())
            .map(st -> caseInsensitiveWord(displayName(st)).thenReturn(st))
            .collect(or());

    private static final Parser<Type> TYPE = Arrays.stream(Type.values())
            .map(t -> caseInsensitiveWord(displayName(t)).thenReturn(t))
            .collect(or());

    @SuppressWarnings("unchecked")
    private static final Parser<Subtype> SUBTYPE = Stream.of(
                    Arrays.stream(CreatureType.values()),
                    Arrays.stream(BasicLandType.values()),
                    Arrays.stream(NonBasicLandType.values()),
                    Arrays.stream(ArtifactType.values()),
                    Arrays.stream(EnchantmentType.values()),
                    Arrays.stream(PlaneswalkerType.values()),
                    Arrays.stream(SpellType.values()))
            .flatMap(s -> s)
            .map(sub -> caseInsensitiveWord(displayName(sub)).thenReturn((Subtype) sub))
            .collect(or());

    /// Parses supertypes and types from the left side of a type line.
    /// Each word is either a supertype or a type; supertypes are tried first.
    private static final Parser<Enum<?>> LEFT_WORD =
            anyOf(SUPERTYPE.map(st -> (Enum<?>) st), TYPE.map(t -> (Enum<?>) t));

    private static final Parser<ParsedTypeLine> TYPE_LINE = LEFT_WORD
            .atLeastOnce()
            .map(words -> new ParsedTypeLine(
                    Supertypes.of(words.stream()
                            .filter(Supertype.class::isInstance)
                            .map(Supertype.class::cast)
                            .toArray(Supertype[]::new)),
                    Types.of(words.stream()
                            .filter(Type.class::isInstance)
                            .map(Type.class::cast)
                            .toArray(Type[]::new)),
                    Subtypes.of()))
            .optionallyFollowedBy(
                    caseInsensitive("—").then(SUBTYPE.atLeastOnce()),
                    (partial, subtypes) -> new ParsedTypeLine(
                            partial.supertypes(), partial.types(), Subtypes.of(subtypes.toArray(Subtype[]::new))));

    /// Parses a type line string into its components.
    ///
    /// @param typeLine the type line to parse
    /// @return the parsed type line components
    public static ParsedTypeLine parse(String typeLine) {
        return TYPE_LINE.parseSkipping(WHITESPACE, typeLine);
    }

    private static String displayName(Enum<?> value) {
        var parts = value.name().split("_");
        var sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append('-');
            sb.append(parts[i].charAt(0));
            sb.append(parts[i].substring(1).toLowerCase(Locale.ROOT));
        }
        return sb.toString();
    }
}
