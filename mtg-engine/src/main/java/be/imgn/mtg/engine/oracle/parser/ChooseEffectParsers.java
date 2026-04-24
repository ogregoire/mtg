package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

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
    static final Parser<Effect.ChooseColor> CHOOSE_COLOR = phrase("Choose a color")
            .thenReturn(new Effect.ChooseColor())
            .optionallyFollowedBy(word("of").then(SubjectParsers.SUBJECT), Effect.ChooseColor::withScope);

    /// "Choose a \[creature|land|…\] type." — type-choice effect that
    /// sets up a "the chosen type" back-reference (Kindred Dominance).
    static final Parser<Effect.ChooseType> CHOOSE_TYPE = phrase("Choose [a|an]")
            .then(anyOf(
                    word("creature").thenReturn(CardType.CREATURE),
                    word("land").thenReturn(CardType.LAND),
                    word("artifact").thenReturn(CardType.ARTIFACT),
                    word("enchantment").thenReturn(CardType.ENCHANTMENT),
                    word("planeswalker").thenReturn(CardType.PLANESWALKER)))
            .followedBy(word("type"))
            .map(Effect.ChooseType::new);

    /// "\[player\] may change any targets of \[spell\]." — e.g., Sideswipe.
    static final Parser<Effect.ChangeAnyTargets> CHANGE_ANY_TARGETS = sequence(
            SubjectParsers.PLAYER_SUBJECT.followedBy(phrase("may change any targets of")),
            SubjectParsers.SUBJECT,
            Effect.ChangeAnyTargets::new);
}
