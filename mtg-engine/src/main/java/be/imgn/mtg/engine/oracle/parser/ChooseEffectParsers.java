package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Ability;
import be.imgn.mtg.engine.oracle.domain.Amount;
import be.imgn.mtg.engine.oracle.domain.CardType;
import be.imgn.mtg.engine.oracle.domain.Effect;

/// Leaf-effect parsers for chooser-driven effects: CHOOSE, CHOOSE_COLOR,
/// CHOOSE_TYPE, CHOOSE_NEW_TARGETS, CHOOSE_PLAYER_VOTE, CHANGE_ANY_TARGETS,
/// CHANGE_THE_TARGET. Extracted from [EffectParsers] to keep that file
/// under the per-file soft limit. Each parser is registered in
/// [EffectParsers]'s [EffectParsers#BASE_EFFECT] / [EffectParsers#CLAUSE]
/// dispatcher.
final class ChooseEffectParsers {
    private ChooseEffectParsers() {}

    /// "\[player\] choose\[s\] how \[other player\] vote\[s\] \[duration\]?." —
    /// vote-direction effect (Grand Warlord Radha / Sovereign Okinec
    /// Ahau). The chooser decides how the other player's vote is cast.
    static final Parser<Effect.ChoosePlayerVote> CHOOSE_PLAYER_VOTE = Parser.sequence(
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("choose(s) how")),
                    SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("vote(s)")),
                    Effect.ChoosePlayerVote::new)
            .optionallyFollowedBy(EffectParsers.DURATION, Effect.ChoosePlayerVote::withDuration);

    /// "Change the target of \[spell\] \[with a single target\]?." —
    /// redirect an in-flight spell's target. The trailing
    /// "with a single target" qualifier is consumed as flavor.
    static final Parser<Effect.ChangeTheTarget> CHANGE_THE_TARGET = phrase("Change the target of")
            .then(SubjectParsers.SUBJECT)
            .map(Effect.ChangeTheTarget::new)
            .optionallyFollowedBy(phrase("with a single target"), (c, _) -> c);

    /// "The new target must be \<subject\>." — Rebound (back-
    /// reference constraint on the preceding ChangeTheTarget).
    static final Parser<Effect.NewTargetMustBe> NEW_TARGET_MUST_BE =
            phrase("The new target must be").then(SubjectParsers.SUBJECT).map(Effect.NewTargetMustBe::new);

    /// "\[player\] may choose new targets for \[spell\]." — e.g., Redirect.
    static final Parser<Effect.ChooseNewTargets> CHOOSE_NEW_TARGETS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may choose new targets for")),
            SubjectParsers.SUBJECT,
            Effect.ChooseNewTargets::new);

    /// "\[chooser\]? choose\[s\] \[selector\] \[at random\]?." — e.g.,
    /// Duneblast ("Choose up to one creature."); Imperial Edict
    /// ("Target opponent chooses a creature they control.").
    /// The chooser-prefixed and imperative forms share the same
    /// [Effect.Choose] shape, with `chooser` set only when oracle
    /// text names one.
    static final Parser<Effect.Choose> CHOOSE = anyOf(
                    sequence(
                            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("choose(s)")),
                            SubjectParsers.SUBJECT,
                            (chooser, what) -> new Effect.Choose(what).withChooser(chooser)),
                    phrase("Choose").then(SubjectParsers.SUBJECT).map(Effect.Choose::new))
            // "from \[it|them\]" — pronominal back-reference to the
            // revealed group of the prior sentence (Night Terrors:
            // "Target player reveals their hand. You choose a
            // nonland card from it."). Consumed as flavor since the
            // pronoun binds at resolution time.
            .optionallyFollowedBy(phrase("from [it|them]"), (c, _) -> c)
            .optionallyFollowedBy(phrase("at random"), (c, _) -> c.asRandom());

    /// "Choose a color \[of \[scope\]\]?." — color-choice effect (Brave
    /// the Elements; Meteor Crater: "Choose a color of a permanent
    /// you control.").
    /// "Choose \[count\] — \n• mode \n• mode" — modal effect embedded
    /// inside an activated or triggered ability body (Inquisitor Exarch:
    /// "When this creature enters, choose one — • You gain 2 life.
    /// • Target opponent loses 2 life."). Distinct from
    /// [OracleParser#MODAL] which produces an [Ability.Modal] at the
    /// paragraph level; this arm produces an [Effect.ChooseModal]
    /// embedded inside an [Ability.TriggeredAbility] / [Ability.ActivatedAbility].
    /// Routes mode bodies through [EffectParsers#CLAUSE] (a [Parser.Rule]) to
    /// avoid the [OracleParser]→[EffectParsers]→[ChooseEffectParsers] static-
    /// init cycle that direct reference of [OracleParser#MODE] would create.
    private static final Parser<Amount> CHOOSE_MODAL_COUNT = anyOf(
            word("one").thenReturn(Amount.exact(1)),
            word("two").thenReturn(Amount.exact(2)),
            word("three").thenReturn(Amount.exact(3)));

    private static final Parser<Ability.Mode> CHOOSE_MODAL_MODE = string("•")
            .then(EffectParsers.CLAUSE)
            .followedBy(string("."))
            .map(effects -> new Ability.Mode(null, effects));

    static final Parser<Effect.ChooseModal> CHOOSE_MODAL = sequence(
            phrase("Choose").then(CHOOSE_MODAL_COUNT).followedBy(string("—")),
            string("\n").then(CHOOSE_MODAL_MODE).atLeastOnce(),
            Effect.ChooseModal::new);

    static final Parser<Effect.ChooseColor> CHOOSE_COLOR = phrase("Choose a color")
            .thenReturn(new Effect.ChooseColor())
            .optionallyFollowedBy(word("of").then(SubjectParsers.SUBJECT), Effect.ChooseColor::withScope);

    /// "Choose a number between \[min\] and \[max\]." — bounded-integer
    /// choice (By Invitation Only). Both bounds are inclusive integers
    /// printed in oracle text; the resulting [Effect.ChooseNumber] is
    /// usually followed in the same paragraph by a "that many" amount
    /// reference.
    static final Parser<Effect.ChooseNumber> CHOOSE_NUMBER = sequence(
            phrase("Choose a number between").then(Parser.digits().<Integer>map(Integer::parseInt)),
            word("and").then(Parser.digits().<Integer>map(Integer::parseInt)),
            Effect.ChooseNumber::new);

    /// "Choose odd or even." — parity chooser (Extinction Event).
    static final Parser<Effect.ChooseQuality> CHOOSE_QUALITY =
            phrase("Choose odd or even").thenReturn(Effect.ChooseQuality.CHOOSE_ODD_OR_EVEN);

    /// "Choose a \[creature|land|…\] type." / "Choose a basic land
    /// type." — type-choice effect that sets up a "the chosen type"
    /// back-reference (Kindred Dominance, Terraformer).
    static final Parser<Effect.ChooseType> CHOOSE_TYPE = phrase("Choose [a|an]")
            .then(Parser.<Effect.ChooseType.Kind>anyOf(
                    // "basic land type" — must precede plain "land" so
                    // the longer match wins.
                    phrase("basic land type").thenReturn(Effect.ChooseType.Kind.BasicLandType.BASIC_LAND_TYPE),
                    word("creature")
                            .thenReturn(
                                    (Effect.ChooseType.Kind) new Effect.ChooseType.Kind.OfCardType(CardType.CREATURE))
                            .followedBy(word("type")),
                    word("land")
                            .thenReturn((Effect.ChooseType.Kind) new Effect.ChooseType.Kind.OfCardType(CardType.LAND))
                            .followedBy(word("type")),
                    word("artifact")
                            .thenReturn(
                                    (Effect.ChooseType.Kind) new Effect.ChooseType.Kind.OfCardType(CardType.ARTIFACT))
                            .followedBy(word("type")),
                    word("enchantment")
                            .thenReturn((Effect.ChooseType.Kind)
                                    new Effect.ChooseType.Kind.OfCardType(CardType.ENCHANTMENT))
                            .followedBy(word("type")),
                    word("planeswalker")
                            .thenReturn((Effect.ChooseType.Kind)
                                    new Effect.ChooseType.Kind.OfCardType(CardType.PLANESWALKER))
                            .followedBy(word("type"))))
            .map(Effect.ChooseType::new)
            // "other than <subtype>" — exclusion clause (Standardize:
            // "Choose a creature type other than Wall."). The excluded
            // subtype lands on [Effect.ChooseType.excluded].
            .optionallyFollowedBy(phrase("other than").then(SelectorParsers.SUBTYPE), Effect.ChooseType::excluding);

    /// "\[player\] may change \[any|the\] targets of \[spell\]." — Sideswipe
    /// ("any"), Goblin Flectomancer ("the"). Both wordings grant the same
    /// retargeting permission (rule 608.2g).
    static final Parser<Effect.ChangeAnyTargets> CHANGE_ANY_TARGETS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may change [any|the] targets of")),
            SubjectParsers.SUBJECT,
            Effect.ChangeAnyTargets::new);
}
