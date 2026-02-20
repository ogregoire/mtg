package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.or;

import java.util.Arrays;

import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.selector.TypeMatcher;
import be.imgn.mtg.parse.Parser;

/// Parser for card types in oracle text ({@mtg.rule 205}).
///
/// Card types are characteristics: creature, artifact, enchantment, etc.
/// When a card type appears alone (e.g., "creature"), the game object type
/// is implicitly permanent.
public final class TypeParser {

    private TypeParser() {}

    /// Parses a single card type (handles both singular and plural forms).
    public static final Parser<Type> TYPE = Arrays.stream(Type.values())
            .map(type -> OracleParser.word(type).thenReturn(type))
            .collect(or());

    /// Parses a single card type as a TypeMatcher (game object type is implicitly permanent).
    public static final Parser<TypeMatcher> SINGLE_TYPE = TYPE.map(TypeMatcher.Single::new);

    /// Parses "artifact or enchantment" style disjunctions (game object type is implicitly permanent).
    public static final Parser<TypeMatcher> TYPE_OR_TYPE = SINGLE_TYPE
            .atLeastOnceDelimitedBy("or")
            .map(types -> types.size() == 1 ? types.getFirst() : new TypeMatcher.Or(types));
}
