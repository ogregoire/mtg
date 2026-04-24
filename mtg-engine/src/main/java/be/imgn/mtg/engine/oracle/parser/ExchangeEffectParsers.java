package be.imgn.mtg.engine.oracle.parser;

import static be.imgn.mtg.engine.oracle.parser.SelectorParsers.ZONE_NAME;
import static be.imgn.mtg.engine.oracle.parser.Words.phrase;
import static com.google.common.labs.parse.Parser.anyOf;
import static com.google.common.labs.parse.Parser.sequence;
import static com.google.common.labs.parse.Parser.string;
import static com.google.common.labs.parse.Parser.word;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle.domain.Effect;
import be.imgn.mtg.engine.oracle.domain.Property;
import be.imgn.mtg.engine.oracle.domain.Subject;
import be.imgn.mtg.engine.oracle.domain.Zone;

/// Leaf-effect parsers for all four exchange effects: control, life
/// totals, life-with-property, zones. Extracted from [EffectParsers]
/// to keep that file under the per-file soft limit.
final class ExchangeEffectParsers {
    private ExchangeEffectParsers() {}

    /// "Exchange control of \[subject\]." — e.g., Switcheroo, Avarice
    /// Totem.
    static final Parser<Effect.ExchangeControl> EXCHANGE_CONTROL =
            phrase("Exchange control of").then(SubjectParsers.SUBJECT).map(Effect.ExchangeControl::new);

    /// "Exchange \[possessive\] \[zone\] and \[zone\]." — Harness Infinity.
    static final Parser<Effect.ExchangeZones> EXCHANGE_ZONES = sequence(
            phrase("Exchange")
                    .then(anyOf(
                            anyOf(word("your"), word("their"), word("its"))
                                    .map(s -> s.equalsIgnoreCase("your")
                                            ? Subject.player(Subject.PlayerRef.YOU)
                                            : Subject.player(Subject.PlayerRef.THEY)),
                            word("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)))),
            ZONE_NAME.followedBy(word("and")),
            ZONE_NAME,
            (player, a, b) -> new Effect.ExchangeZones(player, new Zone.Named(a), new Zone.Named(b)));

    /// "Exchange \[player\]'s life total with \[subject\]'s \[property\]." —
    /// Evra, Halcyon Witness ("Exchange your life total with ~'s
    /// power."); Tree of Perdition ("Exchange target opponent's life
    /// total with this creature's toughness.").
    static final Parser<Effect.ExchangeLifeWithProperty> EXCHANGE_LIFE_WITH_PROPERTY = sequence(
            phrase("Exchange")
                    .then(anyOf(
                            word("your").thenReturn(Subject.player(Subject.PlayerRef.YOU)),
                            word("their").thenReturn(Subject.player(Subject.PlayerRef.THEY)),
                            SubjectParsers.PLAYER_SUBJECTS.followedBy(string("'s"))))
                    .followedBy(phrase("life total")),
            word("with").then(SubjectParsers.SUBJECT).followedBy(string("'s")),
            anyOf(
                    word("power").thenReturn(Property.POWER),
                    word("toughness").thenReturn(Property.TOUGHNESS),
                    word("strength").thenReturn(Property.STRENGTH)),
            Effect.ExchangeLifeWithProperty::new);

    /// "\[players\] exchange life totals." — e.g., Soul Conduit.
    static final Parser<Effect.ExchangeLifeTotals> EXCHANGE_LIFE_TOTALS =
            SubjectParsers.SUBJECT.followedBy(phrase("exchange life totals")).map(Effect.ExchangeLifeTotals::new);
}
