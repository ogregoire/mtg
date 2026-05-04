package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.PlayerRelation;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

/// Parser for [ZoneSelector] — the per-zone wrappers around an
/// [ObjectTypeSelector]. Supports the four classes of zone forms:
///
/// 1. **Owned-zone clause**: `[owner-possessive] hand|library|graveyard`
///    — e.g. "card in your graveyard" → `Graveyard(YOU, Card(...))`.
///    Owner uses a small set of common possessives ("your", "an
///    opponent's", "each player's", …); broader possessive coverage
///    is a future extension.
/// 2. **Shared-zone clause**: `in exile`, `in the command zone`, `on
///    the stack` — no owner.
/// 3. **Battlefield clause**: `on the battlefield` — explicit suffix
///    on a permanent/token noun phrase.
/// 4. **Default**: a bare object-type phrase with no zone clause is
///    wrapped in the natural-default zone for that object type
///    (`Permanent`/`Token` → Battlefield; `Spell`/`Ability`/`Copy` →
///    Stack; `Emblem` → CommandZone). `Card` requires an explicit
///    zone clause — bare "creature card" without a zone fails.
public final class ZoneParser {
    private ZoneParser() {}

    // ── Owner possessives ──────────────────────────────────────────

    /// Possessive forms for the owner of an owned zone. Hand-coded
    /// for the common cases; broader coverage (e.g. "target
    /// opponent's", arbitrary [PlayerSelector] possessivized) is a
    /// future extension.
    private static final Parser<PlayerSelector> POSSESSIVE_OWNER = anyOf(
            phrase("your").thenReturn(new PlayerRelationSelector(PlayerRelation.YOU)),
            phrase("an opponent's").thenReturn(new PlayerRelationSelector(PlayerRelation.OPPONENT)),
            phrase("each opponent's").thenReturn(new PlayerRelationSelector(PlayerRelation.OPPONENT)),
            phrase("a player's").thenReturn(PlayerSelector.Anyone.ANYONE),
            phrase("each player's").thenReturn(PlayerSelector.Anyone.ANYONE),
            phrase("any player's").thenReturn(PlayerSelector.Anyone.ANYONE));

    // ── Owned-zone clauses ─────────────────────────────────────────

    /// `[OBJECT_TYPE] in [POSSESSIVE_OWNER] hand` →
    /// [ZoneSelector.Hand]. The object-type must produce
    /// [ObjectTypeSelector.Card] (the only valid contents for an
    /// owned zone); a parse error surfaces for non-Card noun phrases.
    private static final Parser<ZoneSelector.Hand> HAND = sequence(
            ObjectTypeParser.OBJECT_TYPE.followedBy(phrase("in")),
            POSSESSIVE_OWNER.followedBy(phrase("hand")),
            (of, owner) -> new ZoneSelector.Hand(owner, asCard(of, "hand")));

    private static final Parser<ZoneSelector.Library> LIBRARY = sequence(
            ObjectTypeParser.OBJECT_TYPE.followedBy(phrase("in")),
            POSSESSIVE_OWNER.followedBy(phrase("[library|libraries]")),
            (of, owner) -> new ZoneSelector.Library(owner, asCard(of, "library")));

    private static final Parser<ZoneSelector.Graveyard> GRAVEYARD = sequence(
            ObjectTypeParser.OBJECT_TYPE.followedBy(phrase("in")),
            POSSESSIVE_OWNER.followedBy(phrase("graveyard(s)")),
            (of, owner) -> new ZoneSelector.Graveyard(owner, asCard(of, "graveyard")));

    // ── Shared-zone clauses ────────────────────────────────────────

    private static final Parser<ZoneSelector> EXILE = ObjectTypeParser.OBJECT_TYPE
            .followedBy(phrase("in exile"))
            .map(of -> new ZoneSelector.Exile(asCard(of, "exile")));

    private static final Parser<ZoneSelector> COMMAND_ZONE = ObjectTypeParser.OBJECT_TYPE
            .followedBy(phrase("in the command zone"))
            .map(of -> new ZoneSelector.CommandZone(asCommandZoneContents(of)));

    private static final Parser<ZoneSelector> STACK_EXPLICIT = ObjectTypeParser.OBJECT_TYPE
            .followedBy(phrase("on the stack"))
            .map(of -> new ZoneSelector.Stack(asStackContents(of)));

    private static final Parser<ZoneSelector> BATTLEFIELD_EXPLICIT = ObjectTypeParser.OBJECT_TYPE
            .followedBy(phrase("on the battlefield"))
            .map(of -> new ZoneSelector.Battlefield(asBattlefieldContents(of)));

    // ── Implicit-default ───────────────────────────────────────────

    /// Bare object-type with no zone clause — wrap in the natural
    /// default zone. Card → unsupported (would be ambiguous); Emblem
    /// → CommandZone; Spell/Ability/Copy → Stack;
    /// Permanent/Token → Battlefield.
    private static final Parser<ZoneSelector> IMPLICIT = ObjectTypeParser.OBJECT_TYPE.map(of -> switch (of) {
        case ObjectTypeSelector.Permanent p -> new ZoneSelector.Battlefield(p);
        case ObjectTypeSelector.Token t -> new ZoneSelector.Battlefield(t);
        case ObjectTypeSelector.Spell s -> new ZoneSelector.Stack(s);
        case ObjectTypeSelector.Ability a -> new ZoneSelector.Stack(a);
        case ObjectTypeSelector.Copy c -> new ZoneSelector.Stack(c);
        case ObjectTypeSelector.Emblem e -> new ZoneSelector.CommandZone(e);
        case ObjectTypeSelector.Card ignored ->
            throw new IllegalStateException(
                    "bare 'card' object-type has no implicit zone — oracle text must specify one");
    });

    /// Top-level [ZoneSelector] entry. Explicit zone clauses tried
    /// first (longest-match; the trailing zone phrase disambiguates),
    /// then implicit defaults.
    public static final Parser<ZoneSelector> ZONE_SELECTOR =
            anyOf(HAND, LIBRARY, GRAVEYARD, EXILE, COMMAND_ZONE, STACK_EXPLICIT, BATTLEFIELD_EXPLICIT, IMPLICIT);

    // ── Type narrowing helpers ─────────────────────────────────────

    private static ObjectTypeSelector.Card asCard(ObjectTypeSelector of, String zoneName) {
        if (of instanceof ObjectTypeSelector.Card c) return c;
        throw new IllegalStateException("zone '" + zoneName + "' only accepts cards, got: "
                + of.getClass().getSimpleName());
    }

    private static ZoneSelector.Stack.Contents asStackContents(ObjectTypeSelector of) {
        if (of instanceof ZoneSelector.Stack.Contents c) return c;
        throw new IllegalStateException("stack only accepts spells/abilities/copies, got: "
                + of.getClass().getSimpleName());
    }

    private static ZoneSelector.Battlefield.Contents asBattlefieldContents(ObjectTypeSelector of) {
        if (of instanceof ZoneSelector.Battlefield.Contents c) return c;
        throw new IllegalStateException("battlefield only accepts permanents/tokens, got: "
                + of.getClass().getSimpleName());
    }

    private static ZoneSelector.CommandZone.Contents asCommandZoneContents(ObjectTypeSelector of) {
        if (of instanceof ZoneSelector.CommandZone.Contents c) return c;
        throw new IllegalStateException(
                "command zone only accepts cards/emblems, got: " + of.getClass().getSimpleName());
    }
}
