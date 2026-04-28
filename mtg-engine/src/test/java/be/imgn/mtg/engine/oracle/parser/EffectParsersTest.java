package be.imgn.mtg.engine.oracle.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.google.mu.util.CharPredicate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.oracle.domain.*;

class EffectParsersTest {

    private static final CharPredicate SPACE = CharPredicate.is(' ');

    // ── Destroy ────────────────────────────────────────────────────────────

    @Nested
    class DestroyEffect {

        @Test
        void parsesDestroyTargetCreature() {
            var result = RemovalEffectParsers.DESTROY.parseSkipping(SPACE, "Destroy target creature");
            assertThat(result).isInstanceOf(Effect.Destroy.class);
            var destroy = (Effect.Destroy) result;
            assertThat(destroy.target()).isInstanceOf(Subject.Select.class);
        }

        @Test
        void parsesDestroyTargetArtifact() {
            var result = RemovalEffectParsers.DESTROY.parseSkipping(SPACE, "destroy target artifact");
            assertThat(result).isInstanceOf(Effect.Destroy.class);
        }
    }

    // ── Exile ──────────────────────────────────────────────────────────────

    @Nested
    class ExileEffect {

        @Test
        void parsesExileTargetNonlandPermanent() {
            var result = RemovalEffectParsers.EXILE.parseSkipping(SPACE, "Exile target nonland permanent");
            assertThat(result).isInstanceOf(Effect.Exile.class);
            var exile = (Effect.Exile) result;
            assertThat(exile.exiled()).isInstanceOf(Exiled.Objects.class);
            var objects = (Exiled.Objects) exile.exiled();
            assertThat(objects.subject()).isInstanceOf(Subject.Select.class);
            var select = (Subject.Select) objects.subject();
            assertThat(select.selector().qualifiers())
                    .anySatisfy(q -> assertThat(q).isEqualTo(new Selector.Qualifier.Types(TypeMatcher.NONLAND)));
        }

        @Test
        void parsesExileTargetCreature() {
            var result = RemovalEffectParsers.EXILE.parseSkipping(SPACE, "exile target creature");
            assertThat(result).isInstanceOf(Effect.Exile.class);
        }

        @Test
        void parsesExileAllGraveyards() {
            var result = RemovalEffectParsers.EXILE.parseSkipping(SPACE, "Exile all graveyards");
            assertThat(result).isInstanceOf(Effect.Exile.class);
            var exile = (Effect.Exile) result;
            assertThat(exile.exiled()).isEqualTo(new Exiled.Zones(ZoneName.GRAVEYARD));
        }
    }

    // ── Bounce ────────────────────────────────────────────────────────────

    @Nested
    class BounceEffect {

        @Test
        void parsesReturnToHand() {
            var result = RemovalEffectParsers.BOUNCE.parseSkipping(SPACE, "return target creature to its owner's hand");
            assertThat(result).isInstanceOf(Effect.Bounce.class);
            var bounce = (Effect.Bounce) result;
            assertThat(bounce.to()).isInstanceOf(Zone.Destination.ToHand.class);
        }
    }

    // ── DealDamage ────────────────────────────────────────────────────────

    @Nested
    class DealDamageEffect {

        @Test
        void parsesDealThreeDamageToAnyTarget() {
            var result = DamageEffectParsers.DEAL_DAMAGE.parseSkipping(SPACE, "Deal 3 damage to any target");
            assertThat(result).isInstanceOf(Effect.DealDamage.class);
            var dd = (Effect.DealDamage) result;
            assertThat(dd.amount()).isEqualTo(new Amount.Exact(3));
            assertThat(dd.target()).isInstanceOf(Subject.AnyTarget.class);
        }

        @Test
        void parsesSelfDealsDamage() {
            var result = DamageEffectParsers.DEAL_DAMAGE.parseSkipping(SPACE, "~ deals 2 damage to target creature");
            assertThat(result).isInstanceOf(Effect.DealDamage.class);
            var dd = (Effect.DealDamage) result;
            assertThat(dd.source()).isInstanceOf(Subject.SelfRef.class);
            assertThat(dd.amount()).isEqualTo(new Amount.Exact(2));
            assertThat(dd.target()).isInstanceOf(Subject.Select.class);
        }

        @Test
        void parsesDealDamageVerbForm() {
            var result = DamageEffectParsers.DEAL_DAMAGE.parseSkipping(SPACE, "deal 5 damage to target player");
            assertThat(result).isInstanceOf(Effect.DealDamage.class);
            var dd = (Effect.DealDamage) result;
            assertThat(dd.amount()).isEqualTo(new Amount.Exact(5));
            assertThat(dd.target()).isInstanceOf(Subject.Player.class);
        }
    }

    // ── GainLife ──────────────────────────────────────────────────────────

    @Nested
    class GainLifeEffect {

        @Test
        void parsesYouGainThreeLife() {
            var result = DamageEffectParsers.GAIN_LIFE.parseSkipping(SPACE, "You gain 3 life");
            assertThat(result).isInstanceOf(Effect.GainLife.class);
            var gl = (Effect.GainLife) result;
            assertThat(gl.player()).isInstanceOf(Subject.Player.class);
            var player = (Subject.Player) gl.player();
            assertThat(player.ref()).isEqualTo(PlayerRef.Pronoun.YOU);
            assertThat(gl.amount()).isEqualTo(new Amount.Exact(3));
        }

        @Test
        void parsesYouGainFiveLife() {
            var result = DamageEffectParsers.GAIN_LIFE.parseSkipping(SPACE, "you gain 5 life");
            assertThat(result).isInstanceOf(Effect.GainLife.class);
            var gl = (Effect.GainLife) result;
            assertThat(gl.amount()).isEqualTo(new Amount.Exact(5));
        }
    }

    // ── LoseLife ──────────────────────────────────────────────────────────

    @Nested
    class LoseLifeEffect {

        @Test
        void parsesTargetOpponentLosesTwoLife() {
            var result = DamageEffectParsers.LOSE_LIFE.parseSkipping(SPACE, "Target opponent loses 2 life");
            assertThat(result).isInstanceOf(Effect.LoseLife.class);
            var ll = (Effect.LoseLife) result;
            assertThat(ll.player()).isInstanceOf(Subject.Player.class);
            var player = (Subject.Player) ll.player();
            assertThat(player.ref()).isEqualTo(PlayerRef.targetOpponent());
            assertThat(ll.amount()).isEqualTo(new Amount.Exact(2));
        }
    }

    // ── Draw ──────────────────────────────────────────────────────────────

    @Nested
    class DrawEffect {

        @Test
        void parsesDrawTwoCards() {
            var result = CardManipulationEffectParsers.DRAW.parseSkipping(SPACE, "Draw two cards");
            assertThat(result).isInstanceOf(Effect.Draw.class);
            var draw = (Effect.Draw) result;
            assertThat(draw.amount()).isEqualTo(new Amount.Exact(2));
        }

        @Test
        void parsesTargetPlayerDrawsACard() {
            var result = CardManipulationEffectParsers.DRAW.parseSkipping(SPACE, "Target player draws a card");
            assertThat(result).isInstanceOf(Effect.Draw.class);
            var draw = (Effect.Draw) result;
            assertThat(draw.player()).isInstanceOf(Subject.Player.class);
            var player = (Subject.Player) draw.player();
            assertThat(player.ref()).isEqualTo(PlayerRef.targetPlayer());
            assertThat(draw.amount()).isEqualTo(new Amount.Exact(1));
        }

        @Test
        void parsesYouDrawOneCard() {
            var result = CardManipulationEffectParsers.DRAW.parseSkipping(SPACE, "you draw one card");
            assertThat(result).isInstanceOf(Effect.Draw.class);
            var draw = (Effect.Draw) result;
            assertThat(draw.amount()).isEqualTo(new Amount.Exact(1));
        }
    }

    // ── Discard ───────────────────────────────────────────────────────────

    @Nested
    class DiscardEffect {

        @Test
        void parsesDiscardACard() {
            var result = CardManipulationEffectParsers.DISCARD.parseSkipping(SPACE, "you discard a card");
            assertThat(result).isInstanceOf(Effect.Discard.class);
            var discard = (Effect.Discard) result;
            assertThat(discard.discarded()).isEqualTo(new Discarded.Cards(new Amount.Exact(1)));
        }

        @Test
        void parsesTargetPlayerDiscardsACard() {
            var result = CardManipulationEffectParsers.DISCARD.parseSkipping(SPACE, "Target player discards a card");
            assertThat(result).isInstanceOf(Effect.Discard.class);
        }

        @Test
        void parsesDiscardYourHand() {
            var result = CardManipulationEffectParsers.DISCARD.parseSkipping(SPACE, "Discard your hand");
            assertThat(result).isInstanceOf(Effect.Discard.class);
            var discard = (Effect.Discard) result;
            assertThat(discard.discarded()).isEqualTo(Discarded.Hand.HAND);
        }
    }

    // ── Mill ──────────────────────────────────────────────────────────────

    @Nested
    class MillEffect {

        @Test
        void parsesMillThreeCards() {
            var result = CardManipulationEffectParsers.MILL.parseSkipping(SPACE, "Mill three cards");
            assertThat(result).isInstanceOf(Effect.Mill.class);
            var mill = (Effect.Mill) result;
            assertThat(mill.amount()).isEqualTo(new Amount.Exact(3));
        }

        @Test
        void parsesTargetPlayerMillsTwoCards() {
            var result = CardManipulationEffectParsers.MILL.parseSkipping(SPACE, "Target player mills two cards");
            assertThat(result).isInstanceOf(Effect.Mill.class);
            var mill = (Effect.Mill) result;
            assertThat(mill.amount()).isEqualTo(new Amount.Exact(2));
        }
    }

    // ── Scry / Surveil ────────────────────────────────────────────────────

    @Nested
    class ScryEffect {

        @Test
        void parsesScryTwo() {
            var result = CardManipulationEffectParsers.SCRY.parseSkipping(SPACE, "Scry 2");
            assertThat(result).isInstanceOf(Effect.Scry.class);
            var scry = (Effect.Scry) result;
            assertThat(scry.amount()).isEqualTo(new Amount.Exact(2));
        }
    }

    @Nested
    class SurveilEffect {

        @Test
        void parsesSurveilOne() {
            var result = CardManipulationEffectParsers.SURVEIL.parseSkipping(SPACE, "Surveil 1");
            assertThat(result).isInstanceOf(Effect.Surveil.class);
            var surveil = (Effect.Surveil) result;
            assertThat(surveil.amount()).isEqualTo(new Amount.Exact(1));
        }
    }

    // ── Shuffle ───────────────────────────────────────────────────────────

    @Nested
    class ShuffleEffect {

        @Test
        void parsesShuffle() {
            var result = CardManipulationEffectParsers.SHUFFLE.parseSkipping(SPACE, "Shuffle");
            assertThat(result).isInstanceOf(Effect.Shuffle.class);
        }

        @Test
        void parsesShuffleLowercase() {
            var result = CardManipulationEffectParsers.SHUFFLE.parseSkipping(SPACE, "shuffle");
            assertThat(result).isInstanceOf(Effect.Shuffle.class);
        }
    }

    // ── Tap/Untap ─────────────────────────────────────────────────────────

    @Nested
    class TapUntapEffect {

        @Test
        void parsesTapTargetCreature() {
            var result = TapEffectParsers.TAP.parseSkipping(SPACE, "Tap target creature");
            assertThat(result).isInstanceOf(Effect.Tap.class);
            assertThat(result.target()).isInstanceOf(Subject.Select.class);
        }

        @Test
        void parsesUntapTargetLand() {
            var result = TapEffectParsers.UNTAP.parseSkipping(SPACE, "Untap target land");
            assertThat(result).isInstanceOf(Effect.Untap.class);
            assertThat(result.target()).isInstanceOf(Subject.Select.class);
        }

        @Test
        void tapOrUntapFansOutToPair() {
            var result = TapEffectParsers.CHANGE_TAP_STATES.parseSkipping(SPACE, "Tap or untap target creature");
            assertThat(result).hasSize(2);
            assertThat(result.get(0)).isInstanceOf(Effect.Tap.class);
            assertThat(result.get(1)).isInstanceOf(Effect.Untap.class);
            // Both peer effects share the same target.
            assertThat(((Effect.Tap) result.get(0)).target()).isEqualTo(((Effect.Untap) result.get(1)).target());
        }
    }

    // ── AddCounters ───────────────────────────────────────────────────────

    @Nested
    class AddCountersEffect {

        @Test
        void putsPlusPlusCounterOnTargetCreature() {
            var result =
                    CounterEffectParsers.ADD_COUNTERS.parseSkipping(SPACE, "Put a +1/+1 counter on target creature");
            assertThat(result).isInstanceOf(Effect.AddCounters.class);
            var ac = (Effect.AddCounters) result;
            assertThat(ac.count()).isEqualTo(new Amount.Exact(1));
            assertThat(ac.type()).isEqualTo(new CounterType.PtCounter(1, 1));
            assertThat(ac.target()).isInstanceOf(Subject.Select.class);
        }

        @Test
        void putsTwoChargeCounters() {
            var result = CounterEffectParsers.ADD_COUNTERS.parseSkipping(SPACE, "put two charge counters on ~");
            assertThat(result).isInstanceOf(Effect.AddCounters.class);
            var ac = (Effect.AddCounters) result;
            assertThat(ac.count()).isEqualTo(new Amount.Exact(2));
            assertThat(ac.type()).isEqualTo(CounterType.Named.CHARGE);
        }
    }

    // ── RemoveCounters ────────────────────────────────────────────────────

    @Nested
    class RemoveCountersEffect {

        @Test
        void removesLoyaltyCounterFromPlaneswalker() {
            var result = CounterEffectParsers.REMOVE_COUNTERS.parseSkipping(
                    SPACE, "Remove a loyalty counter from target planeswalker");
            assertThat(result).isInstanceOf(Effect.RemoveCounters.class);
            var rc = (Effect.RemoveCounters) result;
            assertThat(rc.count()).isEqualTo(new Amount.Exact(1));
            assertThat(rc.type()).isEqualTo(CounterType.Named.LOYALTY);
        }
    }

    // ── CounterSpell ──────────────────────────────────────────────────────

    @Nested
    class CounterSpellEffect {

        @Test
        void countersTargetSpell() {
            var result = EffectParsers.COUNTER_SPELL.parseSkipping(SPACE, "Counter target spell");
            assertThat(result).isInstanceOf(Effect.CounterSpell.class);
            var cs = (Effect.CounterSpell) result;
            assertThat(cs.target()).isInstanceOf(Subject.Select.class);
        }
    }

    // ── GainAbility ───────────────────────────────────────────────────────

    @Nested
    class GainAbilityEffect {

        @Test
        void gainsFlying() {
            var result = AbilityGainLoseEffectParsers.GAIN_ABILITY.parseSkipping(
                    SPACE, "Target creature gains flying until end of turn");
            assertThat(result).isInstanceOf(Effect.GainAbility.class);
            var ga = (Effect.GainAbility) result;
            assertThat(ga.abilities()).containsExactly(Ability.StaticKeyword.FLYING);
            assertThat(ga.duration()).isEqualTo(Duration.Fixed.UNTIL_END_OF_TURN);
        }

        @Test
        void gainsTramplAndHasteUntilEndOfTurn() {
            var result = AbilityGainLoseEffectParsers.GAIN_ABILITY.parseSkipping(
                    SPACE, "Target creature gains trample, haste until end of turn");
            assertThat(result).isInstanceOf(Effect.GainAbility.class);
            var ga = (Effect.GainAbility) result;
            assertThat(ga.abilities()).containsExactly(Ability.StaticKeyword.TRAMPLE, Ability.StaticKeyword.HASTE);
        }

        @Test
        void gainsAbilityWithoutDuration() {
            var result = AbilityGainLoseEffectParsers.GAIN_ABILITY.parseSkipping(SPACE, "target creature gains flying");
            assertThat(result).isInstanceOf(Effect.GainAbility.class);
            var ga = (Effect.GainAbility) result;
            assertThat(ga.abilities()).containsExactly(Ability.StaticKeyword.FLYING);
            assertThat(ga.duration()).isNull();
        }
    }

    // ── ModifyPT ──────────────────────────────────────────────────────────

    @Nested
    class ModifyPTEffect {

        @Test
        void getsPlus2Plus2UntilEndOfTurn() {
            var result = EffectParsers.MODIFY_PT.parseSkipping(SPACE, "Target creature gets +2/+2 until end of turn");
            assertThat(result).isInstanceOf(Effect.ModifyPT.class);
            var mpt = (Effect.ModifyPT) result;
            assertThat(mpt.modifier()).isEqualTo(PtModifier.fixed(2, 2));
            assertThat(mpt.duration()).isEqualTo(Duration.Fixed.UNTIL_END_OF_TURN);
        }

        @Test
        void getsMinus1Minus1() {
            var result = EffectParsers.MODIFY_PT.parseSkipping(SPACE, "target creature gets -1/-1 until end of turn");
            assertThat(result).isInstanceOf(Effect.ModifyPT.class);
            var mpt = (Effect.ModifyPT) result;
            assertThat(mpt.modifier()).isEqualTo(PtModifier.fixed(-1, -1));
        }

        @Test
        void getsModifierWithoutDuration() {
            var result = EffectParsers.MODIFY_PT.parseSkipping(SPACE, "target creature gets +1/+0");
            assertThat(result).isInstanceOf(Effect.ModifyPT.class);
            var mpt = (Effect.ModifyPT) result;
            assertThat(mpt.modifier()).isEqualTo(PtModifier.fixed(1, 0));
            assertThat(mpt.duration()).isNull();
        }
    }

    // ── GainControl ───────────────────────────────────────────────────────

    @Nested
    class GainControlEffect {

        @Test
        void gainsControlUntilEndOfTurn() {
            var result = EffectParsers.GAIN_CONTROL.parseSkipping(
                    SPACE, "You gain control of target creature until end of turn");
            assertThat(result).isInstanceOf(Effect.GainControl.class);
            var gc = (Effect.GainControl) result;
            assertThat(gc.player()).isInstanceOf(Subject.Player.class);
            var player = (Subject.Player) gc.player();
            assertThat(player.ref()).isEqualTo(PlayerRef.Pronoun.YOU);
            assertThat(gc.duration()).isEqualTo(Duration.Fixed.UNTIL_END_OF_TURN);
        }
    }

    // ── CreateToken ───────────────────────────────────────────────────────

    @Nested
    class CreateTokenEffect {

        @Test
        void createsTreasureToken() {
            var result = EffectParsers.CREATE_TOKEN.parseSkipping(SPACE, "Create a Treasure token");
            assertThat(result).isInstanceOf(Effect.CreateToken.class);
            var ct = (Effect.CreateToken) result;
            assertThat(ct.count()).isEqualTo(new Amount.Exact(1));
            assertThat(ct.token()).isInstanceOf(TokenDescription.Predefined.class);
            var predefined = (TokenDescription.Predefined) ct.token();
            assertThat(predefined.name()).isEqualTo(PredefinedToken.TREASURE);
        }

        @Test
        void createsTwoWhiteCreatureTokens() {
            var result = EffectParsers.CREATE_TOKEN.parseSkipping(SPACE, "Create two 1/1 white creature tokens");
            assertThat(result).isInstanceOf(Effect.CreateToken.class);
            var ct = (Effect.CreateToken) result;
            assertThat(ct.count()).isEqualTo(new Amount.Exact(2));
            assertThat(ct.token()).isInstanceOf(TokenDescription.Custom.class);
            var custom = (TokenDescription.Custom) ct.token();
            assertThat(custom.pt()).isEqualTo(new PtValue(1, 1));
            assertThat(custom.colors()).containsExactly(Color.WHITE);
        }
    }

    // ── AddMana ───────────────────────────────────────────────────────────

    @Nested
    class AddManaEffect {

        @Test
        void addsGreenMana() {
            var result = EffectParsers.ADD_MANA.parseSkipping(SPACE, "Add {G}");
            assertThat(result).isInstanceOf(Effect.AddMana.class);
            var am = (Effect.AddMana) result;
            assertThat(am.options()).containsExactly(new ManaOption.Fixed(List.of(new ManaSymbol("{G}"))));
        }

        @Test
        void addsWhiteWhiteMana() {
            var result = EffectParsers.ADD_MANA.parseSkipping(SPACE, "Add {W}{W}");
            assertThat(result).isInstanceOf(Effect.AddMana.class);
            var am = (Effect.AddMana) result;
            assertThat(am.options())
                    .containsExactly(new ManaOption.Fixed(List.of(new ManaSymbol("{W}"), new ManaSymbol("{W}"))));
        }

        @Test
        void addsTwoBlackMana() {
            var result = EffectParsers.ADD_MANA.parseSkipping(SPACE, "Add {2}{B}");
            assertThat(result).isInstanceOf(Effect.AddMana.class);
            var am = (Effect.AddMana) result;
            assertThat(am.options())
                    .containsExactly(new ManaOption.Fixed(List.of(new ManaSymbol("{2}"), new ManaSymbol("{B}"))));
        }

        @Test
        void addsThreeManaOfAnyOneColor() {
            var result = EffectParsers.ADD_MANA.parseSkipping(SPACE, "Add three mana of any one color");
            assertThat(result).isInstanceOf(Effect.AddMana.class);
            var am = (Effect.AddMana) result;
            assertThat(am.options()).hasSize(5);
            assertThat(am.options().getFirst())
                    .isEqualTo(new ManaOption.Repeated(new Amount.Exact(3), new ManaSymbol("{W}")));
        }

        @Test
        void addsXManaOfAnyOneColor() {
            var result = EffectParsers.ADD_MANA.parseSkipping(SPACE, "Add X mana of any one color");
            assertThat(result).isInstanceOf(Effect.AddMana.class);
            var am = (Effect.AddMana) result;
            assertThat(am.options()).hasSize(5);
            assertThat(am.options().getFirst())
                    .isEqualTo(new ManaOption.Repeated(Amount.Variable.VARIABLE, new ManaSymbol("{W}")));
        }

        @Test
        void addsAlternativeMana() {
            var result = EffectParsers.ADD_MANA.parseSkipping(SPACE, "Add {B} or {R}");
            assertThat(result).isInstanceOf(Effect.AddMana.class);
            var am = (Effect.AddMana) result;
            assertThat(am.options())
                    .containsExactly(
                            new ManaOption.Fixed(List.of(new ManaSymbol("{B}"))),
                            new ManaOption.Fixed(List.of(new ManaSymbol("{R}"))));
        }
    }

    // ── Transform ────────────────────────────────────────────────────────

    @Nested
    class TransformEffect {

        @Test
        void transformsSelf() {
            var result = EffectParsers.TRANSFORM.parseSkipping(SPACE, "Transform ~");
            assertThat(result).isInstanceOf(Effect.Transform.class);
            var tr = (Effect.Transform) result;
            assertThat(tr.target()).isInstanceOf(Subject.SelfRef.class);
        }
    }

    // ── Fight ────────────────────────────────────────────────────────────

    @Nested
    class FightEffect {

        @Test
        void fightsTargetCreature() {
            var result = EffectParsers.FIGHT.parseSkipping(SPACE, "Target creature fights target creature");
            assertThat(result).isInstanceOf(Effect.Fight.class);
            var fight = (Effect.Fight) result;
            assertThat(fight.a()).isInstanceOf(Subject.Select.class);
            assertThat(fight.b()).isInstanceOf(Subject.Select.class);
        }
    }

    // ── WinGame / LoseGame ────────────────────────────────────────────────

    @Nested
    class WinLoseGameEffect {

        @Test
        void youWinTheGame() {
            var result = EffectParsers.WIN_GAME.parseSkipping(SPACE, "You win the game");
            assertThat(result).isInstanceOf(Effect.WinGame.class);
            var wg = (Effect.WinGame) result;
            assertThat(wg.player()).isInstanceOf(Subject.Player.class);
            var player = (Subject.Player) wg.player();
            assertThat(player.ref()).isEqualTo(PlayerRef.Pronoun.YOU);
        }

        @Test
        void targetPlayerLosesTheGame() {
            var result = EffectParsers.LOSE_GAME.parseSkipping(SPACE, "Target player loses the game");
            assertThat(result).isInstanceOf(Effect.LoseGame.class);
            var lg = (Effect.LoseGame) result;
            assertThat(lg.player()).isInstanceOf(Subject.Player.class);
            var player = (Subject.Player) lg.player();
            assertThat(player.ref()).isEqualTo(PlayerRef.targetPlayer());
        }
    }

    // ── Duration ─────────────────────────────────────────────────────────

    @Nested
    class DurationParser {

        @Test
        void parsesUntilEndOfTurn() {
            var result = EffectParsers.DURATION.parseSkipping(SPACE, "until end of turn");
            assertThat(result).isEqualTo(Duration.Fixed.UNTIL_END_OF_TURN);
        }

        @Test
        void parsesUntilYourNextTurn() {
            var result = EffectParsers.DURATION.parseSkipping(SPACE, "until your next turn");
            assertThat(result).isEqualTo(Duration.Fixed.UNTIL_YOUR_NEXT_TURN);
        }

        @Test
        void parsesUntilEndOfCombat() {
            var result = EffectParsers.DURATION.parseSkipping(SPACE, "until end of combat");
            assertThat(result).isEqualTo(Duration.Fixed.UNTIL_END_OF_COMBAT);
        }

        @Test
        void parsesThisTurn() {
            var result = EffectParsers.DURATION.parseSkipping(SPACE, "this turn");
            assertThat(result).isEqualTo(Duration.Fixed.THIS_TURN);
        }
    }
}
