package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.AMOUNT;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.COUNTER_TYPE;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.SELECTOR;
import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.one;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Cost;
import be.imgn.mtg.engine.oracle.domain.CounterType;
import be.imgn.mtg.engine.oracle.domain.Effect;
import be.imgn.mtg.engine.oracle.domain.Subject;
import be.imgn.mtg.engine.oracle.domain.Zone;

/// Parsers for costs in oracle text.
final class CostParsers {
    private CostParsers() {}

    // ── Cost components ────────────────────────────────────────────────

    static final Parser<Cost.TapSelf> TAP = string("{T}").thenReturn(Cost.TapSelf.TAP_SELF);

    static final Parser<Cost.UntapSelf> UNTAP = string("{Q}").thenReturn(Cost.UntapSelf.UNTAP_SELF);

    static final Parser<Cost.Mana> MANA_COST =
            EffectParsers.MANA_SYMBOL.atLeastOnce().map(Cost.Mana::new);

    /// "Pay {cost}" — explicit-verb mana cost (Bloodthorn Flail:
    /// "Equip—Pay {3} or discard a card."). Parses the same mana-symbol
    /// body as [#MANA_COST] but consumes a leading "Pay" keyword.
    static final Parser<Cost.Mana> PAY_MANA_COST = phrase("Pay").then(MANA_COST);

    static final Parser<Cost.PayLife> PAY_LIFE = anyOf(
            // "Pay half [your|their|its] life[, rounded up|down]" — fractional
            // life payment (Murderous Betrayal). Reuses the shared HALF_LIFE
            // amount, which already handles the rounding suffix. Must precede
            // the flat-AMOUNT arm so "half" doesn't fall through to bare AMOUNT.
            phrase("Pay").then(DamageEffectParsers.HALF_LIFE).map(Cost.PayLife::new),
            phrase("Pay").then(AMOUNT).followedBy(word("life")).map(Cost.PayLife::new));

    /// Sacrifice cost. Accepts either a self-reference (`~`, `this creature`)
    /// or a full subject / selector (`a creature you control`).
    static final Parser<Cost.SacrificePermanent> SACRIFICE_COST =
            phrase("Sacrifice").then(SubjectParsers.SUBJECT).map(Cost.SacrificePermanent::new);

    static final Parser<Cost.DiscardCard> DISCARD_COST = phrase("Discard")
            .then(SubjectParsers.SUBJECT)
            .map(Cost.DiscardCard::new)
            .optionallyFollowedBy(phrase("at random"), (d, _) -> new Cost.DiscardCard(d.what(), true));

    /// "Discard your hand" — whole-hand discard cost (Null Brooch).
    static final Parser<Cost.DiscardHand> DISCARD_HAND_COST =
            phrase("Discard [your|their|his|her|its] hand").thenReturn(Cost.DiscardHand.DISCARD_HAND);

    static final Parser<Cost.TapPermanent> TAP_PERMANENT =
            phrase("Tap").then(SELECTOR).map(Cost.TapPermanent::new);

    /// "Untap \<selector\>" — additional cost (Benthic Explorers).
    /// Distinct from the `{Q}` self-untap symbol parsed by [#UNTAP].
    static final Parser<Cost.UntapPermanent> UNTAP_PERMANENT =
            phrase("Untap").then(SELECTOR).map(Cost.UntapPermanent::new);

    /// "from [possessive] [zone]" suffix used by [#EXILE_COST] — e.g.,
    /// "exile this card from your hand" (Simian Spirit Guide).
    // Matches: "from <zone>"
    private static final Parser<Zone.Source> EXILE_FROM_ZONE = phrase("from [your|their|its|a|any]")
            .then(ZONE_NAME)
            .map(Zone.Named::new)
            .map(Zone.Source::fromZone);

    /// "from a single [zone]" suffix used by [#EXILE_COST] — e.g.,
    /// "Exile two creature cards from a single graveyard" (Night Soil).
    /// The "single" qualifier means all exiled objects must come from the
    /// same zone instance; captured by [Cost.Exile#fromSingle].
    // Matches: "from a single <zone>"
    private static final Parser<Zone.Source> EXILE_FROM_SINGLE_ZONE =
            phrase("from a single").then(ZONE_NAME).map(Zone.Named::new).map(Zone.Source::fromZone);

    static final Parser<Cost.Exile> EXILE_COST = phrase("Exile")
            .then(SubjectParsers.SUBJECT)
            .map(Cost.Exile::new)
            .optionallyFollowedBy(
                    EXILE_FROM_SINGLE_ZONE,
                    Cost.Exile::withFromSingle) // must precede EXILE_FROM_ZONE ("from a" shared prefix)
            .optionallyFollowedBy(EXILE_FROM_ZONE, Cost.Exile::withFrom);

    // Matches: "Remove <amount> [<type>]? counter(s) from <subject>"
    static final Parser<Cost.RemoveCounter> REMOVE_COUNTER = sequence(
            // "Remove <amount>"
            phrase("Remove").then(AMOUNT),
            anyOf(
                    // Typed: "<counter-type> counter(s) from"
                    COUNTER_TYPE.followedBy(phrase("counter(s) from")),
                    // Untyped (O'aka, Traveling Merchant: "Remove a counter from a nonland
                    // permanent you control") — defaults to a generic "any" type.
                    phrase("counter(s) from").thenReturn(CounterType.Any.ANY)),
            // "<subject>"
            SubjectParsers.SUBJECT,
            Cost.RemoveCounter::new);

    /// Planeswalker loyalty cost — "+N", "-N"/"−N" (handled by
    /// [AmountParsers#SIGNED_INT] which accepts both ASCII hyphen and
    /// U+2212 minus), or the unsigned "0" form used for zero-cost
    /// abilities (Gideon, Ally of Zendikar; Jace, Memory Adept). The
    /// signed zero variants "+0" / "-0" never appear in oracle text.
    static final Parser<Cost.Loyalty> LOYALTY_COST =
            anyOf(AmountParsers.SIGNED_INT.map(Cost.Loyalty::new), one('0').thenReturn(new Cost.Loyalty(0)));

    /// "Return [subject] to [poss] owner's hand" — bounce cost
    /// (Broken Fall: "Return this enchantment to its owner's hand:
    /// Regenerate target creature."; Molting Skin). The possessive
    /// is consumed as flavor since owner-scope is implicit in the
    /// bounce.
    static final Parser<Cost.ReturnToHand> RETURN_TO_HAND_COST = phrase("Return")
            .then(SubjectParsers.SUBJECT)
            .followedBy(phrase("to [its|their|your|his|her] owner's hand"))
            .map(Cost.ReturnToHand::new);

    /// "Put a [type] counter on [subject]" — counter placement cost
    /// (Devoted Druid: "Put a -1/-1 counter on this creature: Untap
    /// this creature."). Typed counter required; the untyped
    /// fallback isn't useful at cost position.
    // Matches: "Put <amount> <counter-type> counter(s) on <subject>"
    static final Parser<Cost.AddCounter> ADD_COUNTER_COST = sequence(
            // "Put <amount>"
            phrase("Put").then(AMOUNT),
            // "<counter-type> counter(s) on"
            COUNTER_TYPE.followedBy(phrase("counter(s) on")),
            // "<subject>"
            SubjectParsers.SUBJECT,
            Cost.AddCounter::new);

    /// "Reveal \[N\]|\[a|an\] \[type\]? card(s) from your hand \[that share X\]?"
    /// — reveal cost (Illuminated Folio: "Reveal two cards from your
    /// hand that share a color"; Daring Buccaneer: "reveal a Pirate
    /// card from your hand"). The optional subtype/card-type narrows
    /// the revealed card; the "that share …" tail captures the
    /// constraint as free text until a structured variant is needed.
    /// Trailing constraint on the revealed set. Each arm is a
    /// printed [Cost.Reveal.Constraint] variant; the only one
    /// reachable today is Illuminated Folio's "share a color".
    private static final Parser<Cost.Reveal.Constraint> REVEAL_CONSTRAINT =
            phrase("share a color").thenReturn(Cost.SharedTrait.COLOR);

    static final Parser<Cost.Reveal> REVEAL_COST = phrase("Reveal")
            .then(SELECTOR)
            .followedBy(phrase("from your hand"))
            .map(sel -> new Cost.Reveal(Subject.select(sel)))
            .optionallyFollowedBy(phrase("that").then(REVEAL_CONSTRAINT), Cost.Reveal::withConstraint);

    // ── Single cost component ──────────────────────────────────────────

    /// "Put \[subject\] on \[top|bottom\] of \[your|their|its\] library." —
    /// Leashling: "Put a card from your hand on top of your library:
    /// Return this creature to its owner's hand.". The cost moves a
    /// card from the hand to a position in the library.
    private static final Parser<Zone.Source> PUT_FROM_ZONE =
            phrase("from [your|their|its]").then(ZONE_NAME).map(Zone.Named::new).map(Zone.Source::fromZone);

    private static final Parser<Cost.PutOnLibrary.Position> PUT_LIBRARY_POSITION = anyOf(
            phrase("on top of [your|their|its] library").thenReturn(Cost.PutOnLibrary.Position.TOP),
            phrase("on the bottom of [your|their|its] library").thenReturn(Cost.PutOnLibrary.Position.BOTTOM),
            phrase("on bottom of [your|their|its] library").thenReturn(Cost.PutOnLibrary.Position.BOTTOM));

    static final Parser<Cost.PutOnLibrary> PUT_ON_LIBRARY_COST = anyOf(
            sequence(
                    phrase("Put").then(SubjectParsers.SUBJECT),
                    PUT_FROM_ZONE,
                    PUT_LIBRARY_POSITION,
                    Cost.PutOnLibrary::new),
            sequence(phrase("Put").then(SubjectParsers.SUBJECT), PUT_LIBRARY_POSITION, Cost.PutOnLibrary::new));

    static final Parser<Cost.Mill> MILL_COST = CardManipulationEffectParsers.MILL_NO_PLAYER.map(Cost.Mill::new);

    static final Parser<Cost> COST_COMPONENT = anyOf(
            TAP,
            UNTAP,
            LOYALTY_COST,
            PAY_LIFE,
            PAY_MANA_COST, // must precede PAY_LIFE-fallback/MANA_COST (shares "pay" prefix)
            SACRIFICE_COST,
            DISCARD_HAND_COST, // must precede DISCARD_COST (shares "discard" prefix)
            DISCARD_COST,
            TAP_PERMANENT,
            UNTAP_PERMANENT,
            EXILE_COST,
            REMOVE_COUNTER,
            RETURN_TO_HAND_COST,
            PUT_ON_LIBRARY_COST, // must precede ADD_COUNTER_COST (shares "Put" prefix)
            ADD_COUNTER_COST,
            REVEAL_COST,
            MILL_COST,
            MANA_COST);

    // ── Compound cost: components separated by commas, alternatives by "or" ──

    private static final Parser<Cost> COMMA_LIST = COST_COMPONENT
            .atLeastOnceDelimitedBy(",")
            .map(costs -> costs.size() == 1 ? costs.getFirst() : new Cost.AllOf(costs));

    /// Activation cost — one or more [#COST_COMPONENT]s joined by
    /// commas ([Cost.AllOf]), optionally with "or"-joined
    /// alternatives ([Cost.AnyOf], e.g., Bloodthorn Flail:
    /// "Equip—Pay {3} or discard a card.").
    public static final Parser<Cost> COST_EXPRESSION = COMMA_LIST
            .atLeastOnceDelimitedBy(phrase("or"), Collectors.toUnmodifiableList())
            .map(options -> options.size() == 1 ? options.getFirst() : new Cost.AnyOf(options));

    /// Predicate identifying cost shapes that have no effect-parser
    /// counterpart: mana payments, life payments, multi-cost "or"-
    /// disjunctions, and "AllOf" lists that contain a mana or life
    /// component. Used by [#MAY] to filter ambiguous verbs (bare
    /// `Cost.Exile` / `Cost.SacrificePermanent` / `Cost.DiscardCard`)
    /// down to the [EffectParsers#MAY_DO] path so current ASTs are
    /// preserved for those cards.
    private static boolean isUnambiguousCost(Cost cost) {
        if (cost instanceof Cost.Mana) return true;
        if (cost instanceof Cost.PayLife) return true;
        if (cost instanceof Cost.AnyOf) return true;
        if (cost instanceof Cost.AllOf allOf) {
            return allOf.costs().stream().anyMatch(c -> c instanceof Cost.Mana || c instanceof Cost.PayLife);
        }
        return false;
    }

    /// Forward-declared rule for the `". If you/they do, <effect>"`
    /// continuation. Bound in [EffectParsers]'s trailing static block —
    /// CostParsers initializes before EffectParsers, so a direct
    /// reference to [EffectParsers#IF_DO_CONTINUATION] would NPE during
    /// `MAY`'s static init.
    static final Parser.Rule<Effect> IF_DO_CONTINUATION_RULE = new Parser.Rule<>();

    /// Forward-declared rule for the `". When you/they do, <effect>"`
    /// continuation. Bound in [EffectParsers]'s trailing static block.
    static final Parser.Rule<Effect> WHEN_DO_CONTINUATION_RULE = new Parser.Rule<>();

    /// "\<chooser\> may \[cost\]. \[If/When \<chooser\> do(es), \[ifDone\]\]?"
    /// — optional cost payment in an effect body (rule 118.12).
    /// Reuses [#COST_EXPRESSION], filtered through [#isUnambiguousCost]
    /// so verbs that overlap with effect-imperative parsers (bare
    /// "discard a card" / "sacrifice a creature" / "exile target X")
    /// fall through to `EffectParsers.MAY_DO`. Cards using
    /// genuinely-cost shapes (Inheritance "may pay {3}", Anthropede
    /// "may discard a card or pay {2}", Blood Crypt "may pay 2 life")
    /// take this path.
    public static final Parser<Effect.MayPay> MAY = sequence(
                    SubjectParsers.PLAYER_SUBJECTS.followedBy(word("may")),
                    COST_EXPRESSION.suchThat(CostParsers::isUnambiguousCost, "unambiguous cost"),
                    Effect.MayPay::new)
            .optionallyFollowedBy(IF_DO_CONTINUATION_RULE, Effect.MayPay::withIfDone)
            .optionallyFollowedBy(WHEN_DO_CONTINUATION_RULE, Effect.MayPay::withIfDone);
}
