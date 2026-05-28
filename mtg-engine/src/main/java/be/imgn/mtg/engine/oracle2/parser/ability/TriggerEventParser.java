package be.imgn.mtg.engine.oracle2.parser.ability;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ability.TriggerEvent;
import be.imgn.mtg.engine.oracle2.parser.selector.AbilitySelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.PlayerSelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.SelectorParser;
import be.imgn.mtg.engine.turn.Step;

/// Parser for [TriggerEvent]. Today the grammar covers a starter
/// set wide enough to test triggered abilities end-to-end on
/// `~`-self-referencing cards (Skirk Prospector, Mulldrifter, etc.):
/// `enters`, `dies`, `attacks`, plus a minimal
/// `at the beginning of your upkeep|end step` form.
public final class TriggerEventParser {
    private TriggerEventParser() {}

    /// `[subject] enter(s) [the battlefield]?` — battlefield-entry
    /// trigger ({@mtg.rule 603.6a}). Modern oracle text drops "the
    /// battlefield"; older printings keep it.
    public static final Parser<TriggerEvent.Enters> ENTERS = SelectorParser.SELECTOR
            .followedBy(phrase("enter(s) [the battlefield]?"))
            .map(TriggerEvent.Enters::new);

    /// `[subject] dies` ({@mtg.rule 603.6c}).
    public static final Parser<TriggerEvent.Dies> DIES =
            SelectorParser.SELECTOR.followedBy(phrase("dies")).map(TriggerEvent.Dies::new);

    /// `[subject] attack(s)` ({@mtg.rule 603.2}).
    public static final Parser<TriggerEvent.Attacks> ATTACKS =
            SelectorParser.SELECTOR.followedBy(phrase("attack(s)")).map(TriggerEvent.Attacks::new);

    /// One step name. Today only `upkeep` and `end step` are
    /// supported; richer step coverage lands when more triggers
    /// need it.
    private static final Parser<Step> STEP_NAME =
            anyOf(phrase("upkeep").thenReturn(Step.UPKEEP), phrase("end step").thenReturn(Step.END));

    /// `the beginning of your [step]` — phase/step trigger
    /// ({@mtg.rule 603.2b}). Only the "your" actor is recognised
    /// today; "each player's" and "each opponent's" are deferred.
    public static final Parser<TriggerEvent.AtBeginningOf> AT_BEGINNING_OF =
            phrase("the beginning of your").then(STEP_NAME).map(TriggerEvent.AtBeginningOf::new);

    /// `[player] give(s) a gift` — Bloomburrow gift trigger
    /// (Jolly Gerbils: "Whenever you give a gift, draw a card.").
    public static final Parser<TriggerEvent.GiveAGift> GIVE_A_GIFT = PlayerSelectorParser.PLAYER_SELECTOR
            .followedBy(phrase("give(s) a gift"))
            .map(TriggerEvent.GiveAGift::new);

    /// `[subject] has [keyword]` — state-based trigger
    /// ({@mtg.rule 603.6f}). Student of Elements: "When this
    /// creature has flying, flip it." Reuses
    /// [AbilitySelectorParser#ABILITY_KEYWORD] so every static
    /// keyword (Flying, Trample, Deathtouch, …) participates.
    public static final Parser<TriggerEvent.HasAbility> HAS_ABILITY = sequence(
            SelectorParser.SELECTOR.followedBy(phrase("has")),
            AbilitySelectorParser.ABILITY_KEYWORD,
            TriggerEvent.HasAbility::new);

    /// One trigger event. Order is by specificity: AT_BEGINNING_OF
    /// before the verb arms because "the beginning of …" doesn't
    /// share a prefix with subject grammars; among the verb arms,
    /// order is incidental — each starts with a SUBJECT and the
    /// verb token is the discriminator.
    public static final Parser<TriggerEvent> TRIGGER_EVENT =
            anyOf(AT_BEGINNING_OF, GIVE_A_GIFT, HAS_ABILITY, ENTERS, DIES, ATTACKS);
}
