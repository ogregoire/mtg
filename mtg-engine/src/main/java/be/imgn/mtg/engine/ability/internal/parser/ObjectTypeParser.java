package be.imgn.mtg.engine.ability.internal.parser;

import static be.imgn.mtg.parse.Parser.anyOf;

import be.imgn.mtg.engine.ability.internal.parser.selector.TypeMatcher;
import be.imgn.mtg.parse.Parser;

/// Parser for game object types in oracle text ({@mtg.rule 109}).
///
/// Game object types determine what kind of object is being referenced:
/// permanent, card, spell. When no game object type is specified (e.g., "creature"),
/// it is implicitly a permanent.
public final class ObjectTypeParser {

    private ObjectTypeParser() {}

    private static final Parser<?> CARD_WORD = OracleParser.word("card");

    private static final Parser<?> SPELL_WORD = OracleParser.word("spell");

    /// Parses "permanent" or "permanents" — game object type with no card type restriction.
    public static final Parser<TypeMatcher> PERMANENT =
            OracleParser.word("permanent").thenReturn(new TypeMatcher.Permanent());

    /// Parses "spell" or "spells" — game object type (card on the stack).
    public static final Parser<TypeMatcher> SPELL = OracleParser.word("spell").thenReturn(new TypeMatcher.Spell());

    /// Parses "card" or "cards" — game object type with no card type restriction.
    public static final Parser<TypeMatcher> CARD = OracleParser.word("card").thenReturn(new TypeMatcher.Card());

    /// Parses "token" or "tokens" — a permanent whose source is a token.
    public static final Parser<TypeMatcher> TOKEN = OracleParser.word("token").thenReturn(new TypeMatcher.Token());

    /// Parses "target" as any legal target (creature, player, or planeswalker).
    public static final Parser<TypeMatcher> TARGET = OracleParser.word("target").thenReturn(new TypeMatcher.Target());

    /// Parses "permanent card" — a card with any permanent card type.
    public static final Parser<TypeMatcher> PERMANENT_CARD =
            OracleParser.word("permanent").followedBy(CARD_WORD).thenReturn(new TypeMatcher.CardWithPermanentType());

    /// Parses "permanent spell" — a spell with any permanent card type.
    public static final Parser<TypeMatcher> PERMANENT_SPELL =
            OracleParser.word("permanent").followedBy(SPELL_WORD).thenReturn(new TypeMatcher.SpellWithPermanentType());

    /// Parses "spell card" — a card with any spell card type (instant or sorcery).
    public static final Parser<TypeMatcher> SPELL_CARD =
            OracleParser.word("spell").followedBy(CARD_WORD).thenReturn(new TypeMatcher.CardWithSpellType());

    /// Parses "creature card", "instant or sorcery card" — a card with specific card type(s).
    public static final Parser<TypeMatcher> TYPED_CARD =
            TypeParser.TYPE.atLeastOnceDelimitedBy("or").followedBy(CARD_WORD).map(TypeMatcher.CardWithType::new);

    /// Parses "creature spell", "artifact or enchantment spell" — a spell with specific card type(s).
    public static final Parser<TypeMatcher> TYPED_SPELL =
            TypeParser.TYPE.atLeastOnceDelimitedBy("or").followedBy(SPELL_WORD).map(TypeMatcher.SpellWithType::new);

    /// Parses any type matcher: compound forms first, then bare game object types, then card types.
    public static final Parser<TypeMatcher> TYPE_MATCHER = anyOf(
            PERMANENT_CARD,
            PERMANENT_SPELL,
            SPELL_CARD,
            TYPED_CARD,
            TYPED_SPELL,
            PERMANENT,
            SPELL,
            TOKEN,
            TARGET,
            CARD,
            TypeParser.TYPE_OR_TYPE);
}
