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
/// 1. **Owned-zone clause**: `[in|from] [owner-possessive] hand|library|graveyard`
///    — e.g. "card in your graveyard" / "card from your graveyard"
///    → `Graveyard(YOU, Card(...))`. Both connectors are accepted
///    because oracle text uses each in different contexts (typically
///    `in` for static descriptors, `from` for movement verbs:
///    "Exile a card from your hand", "Return target creature card
///    from your graveyard").
/// 2. **Shared-zone clause**: `in exile`, `in the command zone`, `on
///    the stack` — no owner.
/// 3. **Battlefield clause**: `on the battlefield` — explicit suffix
///    on a permanent/token noun phrase.
/// 4. **Implicit default**: a bare object-type with no zone clause is
///    wrapped in the natural-default zone for that object type
///    (`Permanent`/`Token` → Battlefield; `Spell`/`Ability`/`Copy` →
///    Stack; `Emblem` → CommandZone). `Card` has no intrinsic zone —
///    the calling effect parser supplies a [CardZoneHint] when it
///    knows what zone the card lives in (Discard → Hand, Mill →
///    Library, Search → Library, …). Without a hint, bare "card"
///    fails — see [#NO_HINT].
public final class ZoneParser {
    private ZoneParser() {}

    /// Strategy for wrapping a bare [ObjectTypeSelector.Card] in its
    /// implicit zone. The card itself has no intrinsic zone (CR
    /// 400.1: cards are zone-mobile), so when oracle text says
    /// "discard a card" without an explicit zone clause, the calling
    /// verb parser supplies the hint that says "I default to Hand"
    /// (Discard) or "I default to Library" (Mill, Scry).
    @FunctionalInterface
    public interface CardZoneHint {
        ZoneSelector wrap(ObjectTypeSelector.Card card);
    }

    /// Reject bare cards with no zone — the legacy behavior. Used
    /// when the calling parser has no business consuming a bare card
    /// (e.g. top-level Destroy / Exile, where bare "creature" lands
    /// on Permanent and never on Card).
    public static final CardZoneHint NO_HINT = card -> {
        throw new IllegalStateException(
                "bare 'card' object-type has no implicit zone — oracle text must specify one or "
                        + "the calling effect must provide a CardZoneHint");
    };

    /// Card-in-hand owned by the placeholder [PlayerSelector.Anyone#ANYONE]
    /// — the engine resolves the owner from context (the discarding
    /// player, etc.). Used by Discard.
    public static final CardZoneHint HAND_OF_ANYONE = card -> new ZoneSelector.Hand(PlayerSelector.Anyone.ANYONE, card);

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
            ObjectTypeParser.OBJECT_TYPE.followedBy(phrase("[in|from]")),
            POSSESSIVE_OWNER.followedBy(phrase("hand")),
            (of, owner) -> new ZoneSelector.Hand(owner, asCard(of, "hand")));

    private static final Parser<ZoneSelector.Library> LIBRARY = sequence(
            ObjectTypeParser.OBJECT_TYPE.followedBy(phrase("[in|from]")),
            POSSESSIVE_OWNER.followedBy(phrase("[library|libraries]")),
            (of, owner) -> new ZoneSelector.Library(owner, asCard(of, "library")));

    private static final Parser<ZoneSelector.Graveyard> GRAVEYARD = sequence(
            ObjectTypeParser.OBJECT_TYPE.followedBy(phrase("[in|from]")),
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

    /// Hint-aware [ZoneSelector] entry. Explicit zone clauses tried
    /// first, then the implicit-default fallback that consults
    /// `hint` when the object-type lands on [ObjectTypeSelector.Card].
    public static Parser<ZoneSelector> zoneSelector(CardZoneHint hint) {
        Parser<ZoneSelector> implicit = ObjectTypeParser.OBJECT_TYPE.map(of -> implicitZoneFor(of, hint));
        return anyOf(HAND, LIBRARY, GRAVEYARD, EXILE, COMMAND_ZONE, STACK_EXPLICIT, BATTLEFIELD_EXPLICIT, implicit);
    }

    /// Top-level [ZoneSelector] entry — no hint. Bare "card" without
    /// an explicit zone clause fails here. Verbs that consume cards
    /// from a known zone build their own parser via
    /// [#zoneSelector(CardZoneHint)].
    public static final Parser<ZoneSelector> ZONE_SELECTOR = zoneSelector(NO_HINT);

    private static ZoneSelector implicitZoneFor(ObjectTypeSelector of, CardZoneHint hint) {
        return switch (of) {
            case ObjectTypeSelector.Permanent p -> new ZoneSelector.Battlefield(p);
            case ObjectTypeSelector.Token t -> new ZoneSelector.Battlefield(t);
            case ObjectTypeSelector.Spell s -> new ZoneSelector.Stack(s);
            case ObjectTypeSelector.Ability a -> new ZoneSelector.Stack(a);
            case ObjectTypeSelector.Copy c -> new ZoneSelector.Stack(c);
            case ObjectTypeSelector.Emblem e -> new ZoneSelector.CommandZone(e);
            case ObjectTypeSelector.Card c -> hint.wrap(c);
        };
    }

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
