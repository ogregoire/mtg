package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.CharacterSet;
import com.google.common.labs.parse.Parser;

/// Parsers for the "but only …" timing restrictions that can follow the
/// "Any player may activate this ability" permission modifier (rule
/// 602.5e).
final class AnyPlayerActivationParsers {
    private AnyPlayerActivationParsers() {}

    /// Owner pronoun for "during \[owner\]'s …" restrictions. Oracle text
    /// uses "their" (= the activating player) or "your" (= the
    /// controller of the source). Either resolves to a player subject.
    private static final Parser<Subject> OWNER_POSSESSIVE = anyOf(
            word("their").thenReturn(Subject.player(Subject.PlayerRef.THEY)),
            word("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)));

    /// "during any \[step-name\] step" — each player's step (Infinite
    /// Hourglass / Armageddon Clock: "during any upkeep step").
    private static final Parser<Ability.AnyPlayerActivation> DURING_ANY_STEP = word("any")
            .then(TriggerEventParsers.STEP_NAME)
            .map(step -> new Ability.AnyPlayerActivation.DuringStep(null, true, step));

    /// "during \[owner\]'s \[step-name\] step" — owner-qualified step
    /// (Well of Knowledge: "during their draw step").
    private static final Parser<Ability.AnyPlayerActivation> DURING_OWNER_STEP = sequence(
            OWNER_POSSESSIVE,
            TriggerEventParsers.STEP_NAME,
            (owner, step) -> new Ability.AnyPlayerActivation.DuringStep(owner, false, step));

    /// "during \[owner\]'s turn \[before the end step\]?." — owner-
    /// qualified turn window (Volrath's Dungeon; Mana Cache for the
    /// "before the end step" variant).
    private static final Parser<Ability.AnyPlayerActivation.DuringTurn> DURING_OWNER_TURN = OWNER_POSSESSIVE
            .followedBy(word("turn"))
            .map(owner -> new Ability.AnyPlayerActivation.DuringTurn(owner, false))
            .optionallyFollowedBy(
                    phrase("before the end step"),
                    (dt, _) -> new Ability.AnyPlayerActivation.DuringTurn(dt.owner(), true));

    /// A condition-clause token — like a word but also accepts mana
    /// symbols (`{1}`), apostrophes, and `/` so predicates with
    /// self-name references, cost symbols, and P/T markers round-trip
    /// verbatim.
    private static final Parser<String> CONDITION_TOKEN =
            consecutive(CharacterSet.charsIn("[A-Za-z0-9'{}+/-]"), "condition token");

    /// "if \[predicate\]" — trailing condition on the activation
    /// permission (Lightning Storm: "but only if Lightning Storm is on
    /// the stack.").
    private static final Parser<Ability.AnyPlayerActivation> IF_CONDITION = word("if")
            .then(CONDITION_TOKEN.atLeastOnce().map(ws -> String.join(" ", ws)))
            .map(text -> new Ability.AnyPlayerActivation.IfCondition(Condition.ifCondition(text)));

    /// Body of "but only \[restriction\]" — the timing-restriction
    /// variants. Longer matches come first so shorter prefixes don't
    /// win prematurely.
    private static final Parser<Ability.AnyPlayerActivation> BUT_ONLY_BODY = Parser.<Ability.AnyPlayerActivation>anyOf(
            phrase("as a sorcery").thenReturn(Ability.AnyPlayerActivation.AsSorcery.AS_SORCERY),
            // DURING_OWNER_TURN must precede DURING_OWNER_STEP — "turn"
            // and step-names share the `owner + noun` shape; testing
            // the turn form first avoids the step branch swallowing
            // "turn" as a step name.
            word("during").then(DURING_ANY_STEP),
            word("during").then(DURING_OWNER_TURN),
            word("during").then(DURING_OWNER_STEP),
            IF_CONDITION);

    /// "but only \[restriction\]" — parses only the `but only` tail.
    /// The outer call site (OracleParser) attaches this as an optional
    /// suffix, defaulting to
    /// [Ability.AnyPlayerActivation.Unrestricted#UNRESTRICTED] when
    /// absent.
    static final Parser<Ability.AnyPlayerActivation> ACTIVATION =
            phrase("but only").then(BUT_ONLY_BODY);
}
