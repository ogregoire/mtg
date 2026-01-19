package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;
import static be.imgn.mtg.parse.Parser.string;

import be.imgn.mtg.engine.ability.internal.parser.selector.TypeMatcher;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.parse.Parser;

/// Parser for card types in oracle text.
public final class TypeParser {

    private TypeParser() {}

    /// Parses a single card type (handles both singular and plural forms).
    public static final Parser<Type> TYPE = anyOf(
            anyOf(string("artifacts"), string("artifact")).thenReturn(Type.ARTIFACT),
            anyOf(string("creatures"), string("creature")).thenReturn(Type.CREATURE),
            anyOf(string("enchantments"), string("enchantment")).thenReturn(Type.ENCHANTMENT),
            anyOf(string("instants"), string("instant")).thenReturn(Type.INSTANT),
            anyOf(string("lands"), string("land")).thenReturn(Type.LAND),
            anyOf(string("planeswalkers"), string("planeswalker")).thenReturn(Type.PLANESWALKER),
            anyOf(string("sorceries"), string("sorcery")).thenReturn(Type.SORCERY),
            anyOf(string("battles"), string("battle")).thenReturn(Type.BATTLE));

    /// Parses "permanent" or "permanents" as any permanent type.
    public static final Parser<TypeMatcher> PERMANENT =
            anyOf(string("permanents"), string("permanent")).thenReturn(new TypeMatcher.AnyPermanent());

    /// Parses "spell" or "spells" as any spell.
    public static final Parser<TypeMatcher> SPELL =
            anyOf(string("spells"), string("spell")).thenReturn(new TypeMatcher.AnySpell());

    /// Parses "target" as any legal target (creature, player, or planeswalker).
    public static final Parser<TypeMatcher> ANY_TARGET = string("target").thenReturn(new TypeMatcher.AnyTarget());

    /// Parses a single type as a TypeMatcher.
    public static final Parser<TypeMatcher> SINGLE_TYPE = TYPE.map(TypeMatcher.Single::new);

    /// Parses "artifact or enchantment" style disjunctions.
    public static final Parser<TypeMatcher> TYPE_OR_TYPE = SINGLE_TYPE
            .atLeastOnceDelimitedBy("or")
            .map(types -> types.size() == 1 ? types.getFirst() : new TypeMatcher.Or(types));

    /// Parses any type matcher (permanent, spell, any target, or specific type(s)).
    public static final Parser<TypeMatcher> TYPE_MATCHER = anyOf(PERMANENT, SPELL, ANY_TARGET, TYPE_OR_TYPE);
}
