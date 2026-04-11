package be.imgn.mtg.engine.oracle;

import static be.imgn.mtg.engine.oracle.Words.w;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.consecutive;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import java.util.List;

import com.google.common.labs.parse.Parser;
import com.google.mu.util.CharPredicate;

/// Parsers for effect productions in oracle text.
final class EffectParsers {
    private EffectParsers() {}

    // ── Shared primitives ──────────────────────────────────────────────

    static final Parser<ManaSymbol> MANA_SYMBOL = string("{")
            .then(consecutive(CharPredicate.noneOf(" {}"), "mana content"))
            .followedBy(string("}"))
            .map(s -> new ManaSymbol("{" + s + "}"));

    static final Parser<PtModifier> PT_MODIFIER =
            sequence(SelectorParsers.SIGNED_INT, string("/").then(SelectorParsers.SIGNED_INT), PtModifier::new);

    static final Parser<Duration> DURATION = anyOf(
            w("until").then(w("end").then(w("of")).then(w("turn"))).thenReturn(Duration.untilEndOfTurn()),
            w("until").then(w("your").then(w("next")).then(w("turn"))).thenReturn(Duration.untilYourNextTurn()),
            w("until").then(w("end").then(w("of")).then(w("combat"))).thenReturn(Duration.untilEndOfCombat()),
            w("this").then(w("turn")).thenReturn(Duration.thisTurn()));

    private static final Parser<String> KEYWORD_NAME = anyOf(
            w("first").then(w("strike")).thenReturn("first strike"),
            w("double").then(w("strike")).thenReturn("double strike"),
            w("death").then(w("touch")).thenReturn("deathtouch"),
            w("flying"),
            w("trample"),
            w("haste"),
            w("vigilance"),
            w("lifelink"),
            w("deathtouch"),
            w("hexproof"),
            w("indestructible"),
            w("menace"),
            w("reach"),
            w("defender"),
            w("flash"),
            w("fear"),
            w("intimidate"),
            w("shroud"),
            w("wither"),
            w("infect"),
            w("prowess"),
            word().suchThat(k -> k.length() > 2 && Character.isLowerCase(k.charAt(0)), "keyword name"));

    private static final Parser<List<String>> KEYWORD_LIST = KEYWORD_NAME.atLeastOnceDelimitedBy(",");

    // ── Predefined tokens ──────────────────────────────────────────────

    private static final Parser<TokenDescription> PREDEFINED_TOKEN = anyOf(
                    w("Treasure"),
                    w("Food"),
                    w("Gold"),
                    w("Clue"),
                    w("Blood"),
                    w("Powerstone"),
                    w("Map"),
                    w("Incubator"))
            .followedBy(anyOf(word("tokens"), word("token")))
            .map(TokenDescription::predefined);

    private static final Parser<TokenDescription> CUSTOM_TOKEN = sequence(
            SelectorParsers.PT_VALUE,
            SelectorParsers.COLOR.atLeastOnceDelimitedBy("and"),
            SelectorParsers.CARD_TYPE.atLeastOnce().followedBy(anyOf(word("tokens"), word("token"))),
            TokenDescription.Custom::new);

    private static final Parser<TokenDescription> TOKEN_DESCRIPTION = anyOf(PREDEFINED_TOKEN, CUSTOM_TOKEN);

    // ── Effects ────────────────────────────────────────────────────────

    // Removal

    static final Parser<Effect.Destroy> DESTROY =
            w("destroy").then(SubjectParsers.SUBJECT).map(Effect.Destroy::new);

    static final Parser<Effect.Exile> EXILE =
            w("exile").then(SubjectParsers.SUBJECT).map(Effect.Exile::new);

    static final Parser<Effect.Sacrifice> SACRIFICE = sequence(
            SubjectParsers.PLAYER_SUBJECT,
            anyOf(w("sacrifices"), w("sacrifice")).then(SelectorParsers.SELECTOR),
            Effect.Sacrifice::new);

    static final Parser<Effect.Bounce> BOUNCE =
            sequence(w("return").then(SubjectParsers.SUBJECT), ZoneParsers.ZONE_DESTINATION, Effect.Bounce::new);

    // Damage & Life

    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_SUBJ = sequence(
            SubjectParsers.SUBJECT.followedBy(anyOf(w("deals"), w("deal"))),
            SelectorParsers.AMOUNT.followedBy(w("damage").then(w("to"))),
            SubjectParsers.SUBJECT,
            Effect.DealDamage::new);

    private static final Parser<Effect.DealDamage> DEAL_DAMAGE_VERB = sequence(
            w("deal").then(SelectorParsers.AMOUNT),
            w("damage").then(w("to")).then(SubjectParsers.SUBJECT),
            (amount, target) -> new Effect.DealDamage(Subject.selfRef(null), amount, target));

    static final Parser<Effect.DealDamage> DEAL_DAMAGE = anyOf(DEAL_DAMAGE_SUBJ, DEAL_DAMAGE_VERB);

    static final Parser<Effect.GainLife> GAIN_LIFE = sequence(
            SubjectParsers.PLAYER_SUBJECT,
            anyOf(w("gains"), w("gain")).then(SelectorParsers.AMOUNT).followedBy(w("life")),
            Effect.GainLife::new);

    static final Parser<Effect.LoseLife> LOSE_LIFE = sequence(
            SubjectParsers.PLAYER_SUBJECT,
            anyOf(w("loses"), w("lose")).then(SelectorParsers.AMOUNT).followedBy(w("life")),
            Effect.LoseLife::new);

    // Card Manipulation

    private static final Subject YOU = Subject.player(Subject.PlayerRef.you());

    private static final Parser<Amount> DRAW_NO_PLAYER =
            anyOf(w("draws"), w("draw")).then(SelectorParsers.AMOUNT).followedBy(anyOf(w("cards"), w("card")));

    static final Parser<Effect.Draw> DRAW = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECT, DRAW_NO_PLAYER, Effect.Draw::new),
            DRAW_NO_PLAYER.map(amount -> new Effect.Draw(YOU, amount)));

    private static final Parser<Amount> DISCARD_NO_PLAYER =
            anyOf(w("discards"), w("discard")).then(SelectorParsers.AMOUNT).followedBy(anyOf(w("cards"), w("card")));

    static final Parser<Effect.Discard> DISCARD = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECT, DISCARD_NO_PLAYER, Effect.Discard::new),
            DISCARD_NO_PLAYER.map(amount -> new Effect.Discard(YOU, amount)));

    private static final Parser<Amount> MILL_NO_PLAYER =
            anyOf(w("mills"), w("mill")).then(SelectorParsers.AMOUNT).followedBy(anyOf(w("cards"), w("card")));

    static final Parser<Effect.Mill> MILL = anyOf(
            sequence(SubjectParsers.PLAYER_SUBJECT, MILL_NO_PLAYER, Effect.Mill::new),
            MILL_NO_PLAYER.map(amount -> new Effect.Mill(YOU, amount)));

    static final Parser<Effect.Scry> SCRY =
            anyOf(w("scry"), w("surveil")).then(SelectorParsers.AMOUNT).map(Effect.Scry::new);

    static final Parser<Effect.Search> SEARCH = sequence(
            w("search").then(anyOf(w("your"), w("their"), w("its"))),
            w("library").then(w("for")).then(SelectorParsers.SELECTOR),
            Effect.Search::new);

    static final Parser<Effect.Shuffle> SHUFFLE = w("shuffle").thenReturn(new Effect.Shuffle());

    static final Parser<Effect.Reveal> REVEAL =
            w("reveal").then(SubjectParsers.SUBJECT).map(Effect.Reveal::new);

    // Tap/Untap

    static final Parser<Effect.Tap> TAP = w("tap").then(SubjectParsers.SUBJECT).map(Effect.Tap::new);

    static final Parser<Effect.Untap> UNTAP =
            w("untap").then(SubjectParsers.SUBJECT).map(Effect.Untap::new);

    // Counters

    static final Parser<Effect.AddCounters> ADD_COUNTERS = sequence(
            w("put").then(SelectorParsers.AMOUNT),
            SelectorParsers.COUNTER_TYPE
                    .followedBy(anyOf(w("counters"), w("counter")))
                    .followedBy(w("on")),
            SubjectParsers.SUBJECT,
            Effect.AddCounters::new);

    static final Parser<Effect.RemoveCounters> REMOVE_COUNTERS = sequence(
            w("remove").then(SelectorParsers.AMOUNT),
            SelectorParsers.COUNTER_TYPE
                    .followedBy(anyOf(w("counters"), w("counter")))
                    .followedBy(w("from")),
            SubjectParsers.SUBJECT,
            Effect.RemoveCounters::new);

    // Counterspell

    static final Parser<Effect.CounterSpell> COUNTER_SPELL =
            w("counter").then(SubjectParsers.SUBJECT).map(Effect.CounterSpell::new);

    // Ability modification

    static final Parser<Effect.GainAbility> GAIN_ABILITY = sequence(
                    SubjectParsers.SUBJECT.followedBy(anyOf(w("gains"), w("gain"))),
                    KEYWORD_LIST,
                    Effect.GainAbility::new)
            .optionallyFollowedBy(DURATION, Effect.GainAbility::withDuration);

    // P/T modification

    static final Parser<Effect.ModifyPT> MODIFY_PT = sequence(
                    SubjectParsers.SUBJECT.followedBy(anyOf(w("gets"), w("get"))), PT_MODIFIER, Effect.ModifyPT::new)
            .optionallyFollowedBy(DURATION, Effect.ModifyPT::withDuration);

    // Control

    static final Parser<Effect.GainControl> GAIN_CONTROL = sequence(
                    SubjectParsers.PLAYER_SUBJECT,
                    anyOf(w("gains"), w("gain"))
                            .then(w("control"))
                            .then(w("of"))
                            .then(SubjectParsers.SUBJECT),
                    Effect.GainControl::new)
            .optionallyFollowedBy(DURATION, Effect.GainControl::withDuration);

    // Tokens

    static final Parser<Effect.CreateToken> CREATE_TOKEN =
            sequence(w("create").then(SelectorParsers.AMOUNT), TOKEN_DESCRIPTION, Effect.CreateToken::new);

    // Mana

    static final Parser<Effect.AddMana> ADD_MANA =
            w("add").then(MANA_SYMBOL.atLeastOnce()).map(Effect.AddMana::new);

    // Transform/Copy

    static final Parser<Effect.Transform> TRANSFORM =
            w("transform").then(SubjectParsers.SUBJECT).map(Effect.Transform::new);

    static final Parser<Effect.Copy> COPY =
            w("copy").then(SubjectParsers.SUBJECT).map(Effect.Copy::new);

    // Combat

    static final Parser<Effect.Fight> FIGHT = sequence(
            SubjectParsers.SUBJECT.followedBy(anyOf(w("fights"), w("fight"))),
            SubjectParsers.SUBJECT,
            Effect.Fight::new);

    // Win/Loss

    static final Parser<Effect.WinGame> WIN_GAME = SubjectParsers.PLAYER_SUBJECT
            .followedBy(anyOf(w("wins"), w("win")).then(w("the")).then(w("game")))
            .map(Effect.WinGame::new);

    static final Parser<Effect.LoseGame> LOSE_GAME = SubjectParsers.PLAYER_SUBJECT
            .followedBy(anyOf(w("loses"), w("lose")).then(w("the")).then(w("game")))
            .map(Effect.LoseGame::new);

    // Zone movement

    static final Parser<Effect.ZoneMove> ZONE_MOVE =
            sequence(w("put").then(SubjectParsers.SUBJECT), ZoneParsers.ZONE_DESTINATION, Effect.ZoneMove::new);

    // Prevent

    static final Parser<Effect.Prevent> PREVENT = w("prevent")
            .then(w("the").then(w("next")).then(SelectorParsers.AMOUNT).followedBy(w("damage")))
            .map(amount -> new Effect.Prevent("prevent the next " + amount + " damage"));

    // ── Master dispatcher ──────────────────────────────────────────────

    public static final Parser<Effect> EFFECT = anyOf(
            DESTROY,
            EXILE,
            BOUNCE,
            SACRIFICE,
            DEAL_DAMAGE,
            GAIN_LIFE,
            LOSE_LIFE,
            DRAW,
            DISCARD,
            MILL,
            SCRY,
            SEARCH,
            SHUFFLE,
            REVEAL,
            TAP,
            UNTAP,
            ADD_COUNTERS,
            REMOVE_COUNTERS,
            COUNTER_SPELL,
            GAIN_ABILITY,
            MODIFY_PT,
            GAIN_CONTROL,
            CREATE_TOKEN,
            ADD_MANA,
            TRANSFORM,
            COPY,
            FIGHT,
            WIN_GAME,
            LOSE_GAME,
            ZONE_MOVE,
            PREVENT);
}
