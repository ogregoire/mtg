package be.imgn.mtg.engine.oracle2.parser.effect;

import static be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector.YOU;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle2.domain.Amount;
import be.imgn.mtg.engine.oracle2.domain.CardType;
import be.imgn.mtg.engine.oracle2.domain.Color;
import be.imgn.mtg.engine.oracle2.domain.PlayerRelation;
import be.imgn.mtg.engine.oracle2.domain.StandardQuantifier;
import be.imgn.mtg.engine.oracle2.domain.Supertype;
import be.imgn.mtg.engine.oracle2.domain.effect.DestroyEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.DiscardEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.DrawEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.Effect;
import be.imgn.mtg.engine.oracle2.domain.effect.ExileEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.GainLifeEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.LoseLifeEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.SacrificeEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.ScryEffect;
import be.imgn.mtg.engine.oracle2.domain.effect.SharedSubjectEffect;
import be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ColorSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectPropertySelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerRelationSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.PlayerSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.QuantifierSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.Selector;
import be.imgn.mtg.engine.oracle2.domain.selector.SupertypeSelector;
import be.imgn.mtg.engine.oracle2.domain.selector.ZoneSelector;

class EffectParserTest {

    private static Effect parse(String input) {
        return EffectParser.EFFECT.parseSkipping(CharPredicate.is(' '), input);
    }

    private static final ZoneSelector.Battlefield CREATURE =
            new ZoneSelector.Battlefield(new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.CREATURE)));

    /// Wrap `inner` in `Quantifier(Exact(1), …)` — every parsed
    /// selector carries this wrapper, so test expectations do too.
    private static Selector one(Selector inner) {
        return new QuantifierSelector(new Amount.Exact(1), inner);
    }

    @Nested
    class VerbFirst {
        @Test
        void destroyTargetCreature() {
            assertThat(parse("Destroy target creature."))
                    .isEqualTo(new DestroyEffect(one(new ObjectSelector.Target(CREATURE))));
        }

        @Test
        void exileTargetCreature() {
            assertThat(parse("Exile target creature."))
                    .isEqualTo(new ExileEffect(one(new ObjectSelector.Target(CREATURE))));
        }

        /// "from your hand" goes through the explicit `Hand` arm in
        /// [be.imgn.mtg.engine.oracle2.parser.selector.ZoneParser];
        /// the `card` suffix forces the `Card` object-type. NO_HINT
        /// in the surrounding selector is fine because the explicit
        /// zone clause matches first.
        @Test
        void exileACardFromYourHand() {
            var aCardFromYourHand = new QuantifierSelector(
                    new Amount.Exact(1),
                    new ZoneSelector.Hand(
                            new PlayerRelationSelector(PlayerRelation.YOU),
                            new ObjectTypeSelector.Card(ObjectPropertySelector.Anything.ANYTHING)));
            assertThat(parse("Exile a card from your hand.")).isEqualTo(new ExileEffect(aCardFromYourHand));
        }

        /// "a red creature card from your graveyard" — properties
        /// (red + creature) merge into the `Card`'s `where`; "from
        /// your graveyard" wraps in `Graveyard(YOU, …)`.
        @Test
        void exileARedCreatureCardFromYourGraveyard() {
            var redCreatureCard = new QuantifierSelector(
                    new Amount.Exact(1),
                    new ZoneSelector.Graveyard(
                            new PlayerRelationSelector(PlayerRelation.YOU),
                            new ObjectTypeSelector.Card(new ObjectPropertySelector.AllOf(List.of(
                                    new ColorSelector.Is(Color.RED), new CardTypeSelector.Is(CardType.CREATURE))))));
            assertThat(parse("Exile a red creature card from your graveyard."))
                    .isEqualTo(new ExileEffect(redCreatureCard));
        }

        @Test
        void drawThreeCards() {
            assertThat(parse("Draw three cards.")).isEqualTo(new DrawEffect(one(YOU), new Amount.Exact(3)));
        }

        @Test
        void scry1() {
            assertThat(parse("Scry 1.")).isEqualTo(new ScryEffect(one(YOU), new Amount.Exact(1)));
        }

        @Test
        void scry3() {
            assertThat(parse("Scry 3.")).isEqualTo(new ScryEffect(one(YOU), new Amount.Exact(3)));
        }

        /// `X` resolves through [be.imgn.mtg.engine.oracle2.parser.AmountParser]'s
        /// `X_AMOUNT` atom — the same `AMOUNT` parser used by Draw, so
        /// `Scry X.` is covered without dedicated wiring.
        @Test
        void scryX() {
            assertThat(parse("Scry X.")).isEqualTo(new ScryEffect(one(YOU), Amount.Standard.X));
        }

        /// Implicit-YOU subject — `EffectParser.IMPLICIT_YOU` fills in
        /// when no explicit selector precedes the verb. Confirms the
        /// unified subject-led path covers the bare imperative for
        /// any subject-led verb, not just Draw/Scry.
        @Test
        void sacrificeACreature() {
            assertThat(parse("Sacrifice a creature.")).isEqualTo(new SacrificeEffect(one(YOU), one(CREATURE)));
        }
    }

    @Nested
    class SubjectLed {
        @Test
        void targetPlayerDrawsThreeCards() {
            var subject = one(new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE));
            assertThat(parse("Target player draws three cards."))
                    .isEqualTo(new DrawEffect(subject, new Amount.Exact(3)));
        }

        @Test
        void youGain1Life() {
            assertThat(parse("You gain 1 life.")).isEqualTo(new GainLifeEffect(one(YOU), new Amount.Exact(1)));
        }

        @Test
        void targetPlayerSacrificesANonbasicLand() {
            var subject = one(new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE));
            var nonbasicLand = new QuantifierSelector(
                    new Amount.Exact(1),
                    new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new ObjectPropertySelector.AllOf(List.of(
                                    new SupertypeSelector.IsNot(Supertype.BASIC),
                                    new CardTypeSelector.Is(CardType.LAND))))));
            assertThat(parse("Target player sacrifices a nonbasic land."))
                    .isEqualTo(new SacrificeEffect(subject, nonbasicLand));
        }

        @Test
        void targetOpponentDiscardsABlueCard() {
            var subject = one(new PlayerSelector.Target(new PlayerRelationSelector(PlayerRelation.OPPONENT)));
            var blueCard = new QuantifierSelector(
                    new Amount.Exact(1),
                    new ZoneSelector.Hand(
                            PlayerSelector.Anyone.ANYONE,
                            new ObjectTypeSelector.Card(new ColorSelector.Is(Color.BLUE))));
            assertThat(parse("Target opponent discards a blue card.")).isEqualTo(new DiscardEffect(subject, blueCard));
        }

        @Test
        void targetPlayerScries2() {
            var subject = one(new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE));
            assertThat(parse("Target player scries 2.")).isEqualTo(new ScryEffect(subject, new Amount.Exact(2)));
        }

        /// "each opponent" yields `Quantifier(ALL, OPPONENT)`, not the
        /// `Exact(1)` wrap from `one(...)`. Exercises the inflected
        /// "scries" form against a multi-player subject.
        @Test
        void eachOpponentScries1() {
            var subject =
                    new QuantifierSelector(StandardQuantifier.ALL, new PlayerRelationSelector(PlayerRelation.OPPONENT));
            assertThat(parse("Each opponent scries 1.")).isEqualTo(new ScryEffect(subject, new Amount.Exact(1)));
        }

        /// Locks in that `X` flows through the subject-led path too,
        /// not just the verb-first imperative.
        @Test
        void targetOpponentScriesX() {
            var subject = one(new PlayerSelector.Target(new PlayerRelationSelector(PlayerRelation.OPPONENT)));
            assertThat(parse("Target opponent scries X.")).isEqualTo(new ScryEffect(subject, Amount.Standard.X));
        }

        /// "at random" ({@mtg.rule 701.8d}) sets the boolean flag on
        /// [DiscardEffect] — the game (not the discarding player)
        /// picks the card.
        @Test
        void targetPlayerDiscardsACardAtRandom() {
            var subject = one(new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE));
            var aCard = new QuantifierSelector(
                    new Amount.Exact(1),
                    new ZoneSelector.Hand(
                            PlayerSelector.Anyone.ANYONE,
                            new ObjectTypeSelector.Card(ObjectPropertySelector.Anything.ANYTHING)));
            assertThat(parse("Target player discards a card at random."))
                    .isEqualTo(new DiscardEffect(subject, aCard, true));
        }
    }

    @Nested
    class SharedSubjectDistribution {
        /// Two-verb fan-out — sacrificed artifact + gained life share
        /// one player subject. The shared subject lives once at the
        /// [SharedSubjectEffect] root; each clause references it via
        /// [PlayerSelector.SharedSubject], guaranteeing structurally
        /// that a single target was chosen (CR 115.1).
        @Test
        void targetPlayerSacrificesAnArtifactAndGains1Life() {
            var subject = one(new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE));
            var anArtifact = new QuantifierSelector(
                    new Amount.Exact(1),
                    new ZoneSelector.Battlefield(
                            new ObjectTypeSelector.Permanent(new CardTypeSelector.Is(CardType.ARTIFACT))));
            var shared = PlayerSelector.SharedSubject.INSTANCE;
            var expected = new SharedSubjectEffect(
                    subject,
                    List.of(new SacrificeEffect(shared, anArtifact), new GainLifeEffect(shared, new Amount.Exact(1))));
            assertThat(parse("Target player sacrifices an artifact and gains 1 life."))
                    .isEqualTo(expected);
        }

        /// Unscrupulous Contractor's effect fragment — two distinct
        /// effect arms (Draw + LoseLife) sharing one target player.
        /// Locks in the cross-verb-type fan-out and the LoseLifeEffect
        /// addition.
        @Test
        void unscrupulousContractor() {
            var subject = one(new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE));
            var shared = PlayerSelector.SharedSubject.INSTANCE;
            var expected = new SharedSubjectEffect(
                    subject,
                    List.of(
                            new DrawEffect(shared, new Amount.Exact(2)),
                            new LoseLifeEffect(shared, new Amount.Exact(2))));
            assertThat(parse("Target player draws two cards and loses 2 life.")).isEqualTo(expected);
        }

        /// Draw + Scry fan-out — the canonical "draw a card, then
        /// scry" rider compressed into one subject. Verifies scry
        /// composes with another verb under one chosen subject.
        @Test
        void targetPlayerDrawsACardAndScries1() {
            var subject = one(new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE));
            var shared = PlayerSelector.SharedSubject.INSTANCE;
            var expected = new SharedSubjectEffect(
                    subject,
                    List.of(new DrawEffect(shared, new Amount.Exact(1)), new ScryEffect(shared, new Amount.Exact(1))));
            assertThat(parse("Target player draws a card and scries 1.")).isEqualTo(expected);
        }

        /// Single-verb form for the new LoseLifeEffect — exercises
        /// the singleton `distribute` branch (no SharedSubjectEffect
        /// wrap, subject inlined into the bare effect).
        @Test
        void targetPlayerLoses3Life() {
            var subject = one(new PlayerSelector.Target(PlayerSelector.Anyone.ANYONE));
            assertThat(parse("Target player loses 3 life."))
                    .isEqualTo(new LoseLifeEffect(subject, new Amount.Exact(3)));
        }
    }
}
