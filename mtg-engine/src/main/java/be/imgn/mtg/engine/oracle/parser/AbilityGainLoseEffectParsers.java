package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.EffectParsers.AS_LONG_AS_PREFIX;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.DURATION;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.DURING_OTHERS_TURN;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.DURING_YOUR_TURN;
import static be.imgn.mtg.engine.oracle.parser.EffectParsers.UNTIL_END_OF_TURN_PREFIX_INLINE;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Ability;
import be.imgn.mtg.engine.oracle.domain.Effect;

/// Leaf-effect parsers for ability gain / loss: GAIN_ABILITY, LOSE_ABILITY
/// and their supporting lists (KEYWORD_OR_QUOTED, LOST_ABILITIES).
/// Extracted from [EffectParsers] to keep that file under the per-file
/// soft limit.
final class AbilityGainLoseEffectParsers {
    private AbilityGainLoseEffectParsers() {}

    /// One "ability" term in a mixed list — either a single keyword or
    /// a quoted ability block. Used by [#GAIN_ABILITY_CORE] so a
    /// gain-ability clause can mix the two (Prophetic Ravings:
    /// `Enchanted creature has haste and "{T}, Discard a card: Draw
    /// a card."`). Package-visible because [EffectParsers.objectVerbBody]
    /// could potentially reuse it in a chain body.
    static final Parser<Ability> KEYWORD_OR_QUOTED =
            anyOf(OracleParser.ABILITY.optionallyFollowedBy(".").between("\"", "\""), KeywordParsers.KEYWORD);

    /// Delimiter for a mixed keyword / quoted-ability list. Accepts
    /// the same punctuation set as the bare KEYWORD_LIST: `,`, `; `,
    /// `and`, `, and`, `or`, `, or`.
    private static final Parser<String> KEYWORD_OR_QUOTED_DELIM = anyOf(
            sequence(string(","), phrase("and"), (_, _) -> ", and"),
            sequence(string(","), word("or"), (_, _) -> ", or"),
            string(","),
            string(";"),
            phrase("and"),
            word("or"));

    /// Oxford-comma list of [#KEYWORD_OR_QUOTED] — a mixed ability list
    /// shared by [#GAIN_ABILITY_CORE] and [EffectParsers.objectVerbBody]'s
    /// shared-subject "has/gains" chain body (Skeletal Grimace:
    /// "Enchanted creature gets +1/+1 and has '{B}: Regenerate this
    /// creature.'").
    static final Parser<List<Ability>> KEYWORD_OR_QUOTED_LIST =
            KEYWORD_OR_QUOTED.atLeastOnceDelimitedBy(KEYWORD_OR_QUOTED_DELIM, Collectors.toUnmodifiableList());

    /// `\[subject\] \[gains|gain|has|have\] \[abilities\]` — the no-duration
    /// gain-ability core. Consumers prepend optional duration prefixes
    /// (`Until end of turn,`, `During your turn,`, …) and attach
    /// optional trailing DURATION.
    static final Parser<Effect.GainAbility> GAIN_ABILITY_CORE = Parser.sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("[gains|gain|has|have]")),
            KEYWORD_OR_QUOTED_LIST,
            Effect.GainAbility::new);

    static final Parser<Effect.GainAbility> GAIN_ABILITY = anyOf(
                    sequence(DURING_YOUR_TURN, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    // "During turns other than yours, [subject] have
                    // [ability]." — Oak Street Innkeeper.
                    sequence(DURING_OTHERS_TURN, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    sequence(AS_LONG_AS_PREFIX, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    // "Until end of turn, [subject] gain[s] [ability]." —
                    // Shoving Match: "Until end of turn, all creatures gain
                    // '{T}: Tap target creature.'"
                    sequence(UNTIL_END_OF_TURN_PREFIX_INLINE, GAIN_ABILITY_CORE, (d, g) -> g.withDuration(d)),
                    GAIN_ABILITY_CORE)
            .optionallyFollowedBy(DURATION, Effect.GainAbility::withDuration);

    /// "\[subject\] gain\[s\] your choice of \[ability list\] \[duration\]?" —
    /// Assassin Initiate. The chooser ("your") picks one option from the
    /// list at resolution. Must precede [#GAIN_ABILITY] in the dispatcher
    /// since both share the "[subject] gains" prefix.
    static final Parser<Effect.GainAbilityChoice> GAIN_ABILITY_CHOICE = sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("[gains|gain|has|have] your choice of")),
                    KEYWORD_OR_QUOTED_LIST,
                    Effect.GainAbilityChoice::new)
            .optionallyFollowedBy(DURATION, Effect.GainAbilityChoice::withDuration);

    /// "\[subject\] has all \[kind\] abilities of \[selector\]" — copies
    /// every ability of a kind from a live selector (Robaran Mercenaries:
    /// "This creature has all activated abilities of all legendary
    /// creatures you control."). Distinct from [#GAIN_ABILITY] which
    /// grants specific named abilities; here the gained ability set is
    /// dynamic at evaluation time.
    static final Parser<Effect.HasAllAbilitiesOf> HAS_ALL_ABILITIES_OF = sequence(
            SubjectParsers.SUBJECT.followedBy(phrase("[has|have] all")),
            anyOf(
                            word("activated").thenReturn(Effect.HasAllAbilitiesOf.AbilityKind.ACTIVATED),
                            word("triggered").thenReturn(Effect.HasAllAbilitiesOf.AbilityKind.TRIGGERED),
                            word("static").thenReturn(Effect.HasAllAbilitiesOf.AbilityKind.STATIC))
                    .followedBy(phrase("abilities of")),
            SelectorParsers.SELECTOR,
            Effect.HasAllAbilitiesOf::new);

    /// The tail of a "\[subject\] lose\[s\] …" clause. Either "all
    /// abilities" (produces [Effect.LoseAbility.Lost.All]) or a
    /// keyword list (produces [Effect.LoseAbility.Lost.Specific]).
    private static final Parser<Effect.LoseAbility.Lost> LOST_ABILITIES = anyOf(
            // "all landwalk abilities" — Hammerheim. Must precede the
            // plain "all abilities" arm (neither prefix is longer, but
            // the family form is strictly more specific).
            phrase("All landwalk abilities")
                    .thenReturn(new Effect.LoseAbility.Lost.AllInFamily(Effect.LoseAbility.Lost.Family.LANDWALK)),
            // "all abilities except mana abilities" — Blood Sun.
            // Must precede plain "all abilities" so the longer suffix
            // wins.
            phrase("All abilities except mana abilities")
                    .thenReturn(Effect.LoseAbility.Lost.AllExceptMana.ALL_EXCEPT_MANA),
            phrase("All abilities").thenReturn(Effect.LoseAbility.Lost.All.ALL),
            // "all \"<quoted name>\" abilities" — named-keyword family
            // (Shelkin Brownie: "Target creature loses all \"bands
            // with other\" abilities until end of turn.").
            phrase("All")
                    .then(Parser.quotedBy('"', '"'))
                    .followedBy(word("abilities"))
                    .<Effect.LoseAbility.Lost>map(Effect.LoseAbility.Lost.Named::new),
            // "your choice of <keyword list>" — chooser-tagged
            // pick-one (Walking Sponge: "loses your choice of flying,
            // first strike, or trample until end of turn."). Distinct
            // surface form from the bare "X or Y" Urborg pattern, but
            // the same ChooseOne semantics. Uses orList so the
            // ", or" delimiters match.
            phrase("Your choice of")
                    .then(MtgParsers.orList(KeywordParsers.KEYWORD))
                    .map(Effect.LoseAbility.Lost.ChooseOne::new),
            // "<keyword> or <keyword>" — chooser-picks-one form
            // (Urborg: "Target creature loses first strike or
            // swampwalk until end of turn."). Must precede the
            // plain KEYWORD_LIST since orList is more specific.
            MtgParsers.orList(KeywordParsers.KEYWORD)
                    .suchThat(l -> l.size() >= 2, "or-list of keywords")
                    .map(Effect.LoseAbility.Lost.ChooseOne::new),
            KeywordParsers.KEYWORD_LIST.map(Effect.LoseAbility.Lost.Specific::new));

    static final Parser<Effect.LoseAbility> LOSE_ABILITY = Parser.sequence(
                    SubjectParsers.SUBJECT.followedBy(phrase("lose(s)")), LOST_ABILITIES, Effect.LoseAbility::new)
            .optionallyFollowedBy(DURATION, Effect.LoseAbility::withDuration);
}
