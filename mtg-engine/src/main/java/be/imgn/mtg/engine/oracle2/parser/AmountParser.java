package be.imgn.mtg.engine.oracle2.parser;

import static be.imgn.mtg.engine.oracle2.parser.NumberParser.INTEGER;
import static be.imgn.mtg.engine.oracle2.parser.NumberParser.NUMBER;
import static be.imgn.mtg.engine.oracle2.parser.NumberParser.WORD_NUMBER;
import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import java.util.function.Function;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.SelfSelector;
import be.imgn.mtg.engine.oracle2.parser.selector.ObjectSelectorParser;
import be.imgn.mtg.engine.oracle2.parser.selector.PlayerSelectorParser;

/// Parsers for [Amount] expressions, built on the integer/word-number
/// atoms in [NumberParser]. Lives in `oracle2.parser` (not `selector`)
/// because numeric expressions are used across many oracle2 grammars
/// beyond selectors (effect costs, damage values, X definitions, etc.).
public final class AmountParser {
    private AmountParser() {}

    /// `Amount.Standard.X` — variable amount bound by the spell or
    /// ability's cost ({@mtg.rule 107.3}).
    private static final Parser<Amount> X_AMOUNT = word("X").thenReturn(Amount.Standard.X);

    /// `Amount.Standard.REFERENCE` — back-reference to a count
    /// established earlier (e.g. damage just dealt, cards just drawn).
    /// Spelled as "that much", "that many", or "that number" in oracle
    /// text — all collapse to the same domain value.
    private static final Parser<Amount> REFERENCE =
            phrase("that [much|many|number]").thenReturn(Amount.Standard.REFERENCE);

    /// "a" / "an" — exactly one. Title-or-lower per [Parsers#phrase].
    private static final Parser<Amount> ONE = phrase("a(n)").thenReturn(new Amount.Exact(1));

    /// Atoms allowed inside an [Amount.UpTo] bound — only literals and
    /// `X`. No nested `UpTo`, no `Range`, no `REFERENCE`.
    private static final Parser<Amount> UP_TO_INNER = anyOf(NUMBER.map(Amount.Exact::new), X_AMOUNT);

    /// "up to N" / "up to X" → [Amount.UpTo]`(Exact(N))` /
    /// `UpTo(Standard.X)`.
    private static final Parser<Amount.UpTo> UP_TO =
            phrase("up to").then(UP_TO_INNER).map(Amount.UpTo::new);

    /// "N or M" — inclusive range bounded on both sides.
    private static final Parser<Amount.Range> RANGE =
            sequence(NUMBER, word("or").then(NUMBER), Amount.Range::new);

    /// Bare [Amount] entry (no `plus N` tail). Used inside the [#AMOUNT]
    /// composition so the optional `plus` suffix can attach to a
    /// single base amount rather than nest recursively. Order matters:
    /// ranged forms ("N or M") must precede bare integer forms so the
    /// trailing "or M" isn't left hanging for an outer "or".
    private static final Parser<Amount> BASE_AMOUNT = anyOf(
            UP_TO, RANGE, X_AMOUNT, REFERENCE, ONE, WORD_NUMBER.map(Amount.Exact::new), INTEGER.map(Amount.Exact::new));

    /// Top-level [Amount] entry — [#BASE_AMOUNT] with an optional
    /// `plus N` tail ({@link Amount.Plus}). Vitalizing Cascade ("You
    /// gain X plus 3 life.").
    public static final Parser<Amount> AMOUNT =
            BASE_AMOUNT.optionallyFollowedBy(phrase("plus").then(INTEGER), (base, n) -> new Amount.Plus(base, n));

    // ── Described amounts: for-each + property-of ─────────────────────
    //
    // Exported separately (not folded into AMOUNT) because they would
    // shadow the AMOUNT-led dispatch in other grammars — callers that
    // need them pick them up by reference.

    /// "for each \<subject\>" → [Amount.CountOf]. The subject is the
    /// broad [Selector] because real oracle text mixes object-side
    /// ("for each creature you control", "for each card revealed
    /// this way") and player-side ("for each opponent"). Tries the
    /// player parser first so "opponent" / "player" isn't reinterpreted
    /// as an object noun.
    public static final Parser<Amount> FOR_EACH = phrase("for each")
            .then(Parser.<Selector>anyOf(PlayerSelectorParser.PLAYER_SELECTOR, ObjectSelectorParser.OBJECT_SELECTOR))
            .<Amount>map(Amount.CountOf::new);

    /// "the number of \<subject\>" → [Amount.CountOf]. Sibling of
    /// [#FOR_EACH] — same domain shape, different surface phrase.
    /// Used inside `where X is …` clauses and as a standalone amount
    /// expression ("Draw cards equal to the number of …").
    public static final Parser<Amount> THE_NUMBER_OF = phrase("the number of")
            .then(Parser.<Selector>anyOf(PlayerSelectorParser.PLAYER_SELECTOR, ObjectSelectorParser.OBJECT_SELECTOR))
            .<Amount>map(Amount.CountOf::new);

    /// Self-referential possessive subjects ("this creature's", "this
    /// permanent's", …) — all resolve to [SelfSelector#SELF]. Other
    /// possessive forms (target-of, back-reference) land as new arms
    /// when oracle text drives them; this list covers the common
    /// "this <object-type>'s" surface forms with the shared "this"
    /// prefix factored out.
    private static final Parser<ObjectSelector> POSSESSIVE_SUBJECT = word("this")
            .then(anyOf(
                    word("creature's"),
                    word("permanent's"),
                    word("spell's"),
                    word("artifact's"),
                    word("enchantment's"),
                    word("land's")))
            .<ObjectSelector>thenReturn(SelfSelector.SELF);

    /// Property tail of "equal to \<subject\>'s \<property\>" — emits a
    /// builder that wraps the parsed subject in the matching arm.
    private static final Parser<Function<ObjectSelector, Amount>> PROPERTY_TAIL = anyOf(
            phrase("power").<Function<ObjectSelector, Amount>>thenReturn(Amount.PowerOf::new),
            phrase("toughness").<Function<ObjectSelector, Amount>>thenReturn(Amount.ToughnessOf::new),
            phrase("mana value").<Function<ObjectSelector, Amount>>thenReturn(Amount.ManaValueOf::new));

    /// "equal to \<subject\>'s \<property\>" → [Amount.PowerOf] /
    /// [Amount.ToughnessOf] / [Amount.ManaValueOf]. Today only the
    /// `this <object-type>'s` possessive shapes are wired (covers
    /// Viridian Joiner and similar self-referential cards); broader
    /// possessive support lands as cards drive it.
    public static final Parser<Amount> PROPERTY_OF_AMOUNT =
            sequence(phrase("equal to").then(POSSESSIVE_SUBJECT), PROPERTY_TAIL, (sel, fn) -> fn.apply(sel));
}
