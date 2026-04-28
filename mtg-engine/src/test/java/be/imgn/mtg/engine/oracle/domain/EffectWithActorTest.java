package be.imgn.mtg.engine.oracle.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/// Pins the semantics of [Effect#withActor]: rebind the actor field
/// only where it currently holds the YOU placeholder; leave non-YOU
/// actors untouched; cascade through wrapper variants.
class EffectWithActorTest {

    private static final Subject YOU = Subject.player(Subject.PlayerRef.YOU);
    private static final Subject TARGET_PLAYER = Subject.player(Subject.PlayerRef.TARGET_PLAYER);
    private static final Subject TARGET_OPPONENT = Subject.player(Subject.PlayerRef.TARGET_OPPONENT);

    @Nested
    class LeafOverrides {

        @Test
        void rebindsYouOnDraw() {
            var draw = new Effect.Draw(YOU, Amount.exact(1));
            assertThat(draw.withActor(TARGET_PLAYER)).isEqualTo(new Effect.Draw(TARGET_PLAYER, Amount.exact(1)));
        }

        @Test
        void preservesNonYouOnDraw() {
            var draw = new Effect.Draw(TARGET_PLAYER, Amount.exact(1));
            assertThat(draw.withActor(YOU)).isSameAs(draw);
        }

        @Test
        void destroyHasNoActorField() {
            var destroy = new Effect.Destroy(Subject.anyTarget(), null, false);
            assertThat(destroy.withActor(TARGET_PLAYER)).isSameAs(destroy);
        }

        @Test
        void createTokenWithNullCreatorRebindsToActor() {
            var token = new Effect.CreateToken(Amount.exact(1), TokenDescription.predefined(PredefinedToken.TREASURE));
            var rebound = (Effect.CreateToken) token.withActor(TARGET_PLAYER);
            assertThat(rebound.creator()).isEqualTo(TARGET_PLAYER);
        }
    }

    @Nested
    class WrapperCascade {

        @Test
        void conditionalRecursesIntoEffect() {
            var inner = new Effect.Draw(YOU, Amount.exact(1));
            var conditional = new Effect.Conditional(inner, Condition.YouDidIt.YOU_DID_IT);
            assertThat(conditional.withActor(TARGET_PLAYER))
                    .isEqualTo(new Effect.Conditional(
                            new Effect.Draw(TARGET_PLAYER, Amount.exact(1)), Condition.YouDidIt.YOU_DID_IT));
        }

        @Test
        void oneOfRecursesIntoEachAlternative() {
            var oneOf = new Effect.OneOf(
                    List.of(new Effect.Draw(YOU, Amount.exact(1)), new Effect.Mill(YOU, Amount.exact(1))));
            assertThat(oneOf.withActor(TARGET_PLAYER))
                    .isEqualTo(new Effect.OneOf(List.of(
                            new Effect.Draw(TARGET_PLAYER, Amount.exact(1)),
                            new Effect.Mill(TARGET_PLAYER, Amount.exact(1)))));
        }

        @Test
        void mayDoRecursesAndRebindsChooser() {
            var mayDo = new Effect.MayDo(YOU, new Effect.Draw(YOU, Amount.exact(1)), null);
            var rebound = (Effect.MayDo) mayDo.withActor(TARGET_PLAYER);
            assertThat(rebound.chooser()).isEqualTo(TARGET_PLAYER);
            assertThat(rebound.action()).isEqualTo(new Effect.Draw(TARGET_PLAYER, Amount.exact(1)));
        }

        @Test
        void mayDoLeavesNonYouChooserUntouched() {
            var mayDo = new Effect.MayDo(TARGET_OPPONENT, new Effect.Draw(TARGET_OPPONENT, Amount.exact(1)), null);
            var rebound = (Effect.MayDo) mayDo.withActor(YOU);
            assertThat(rebound.chooser()).isEqualTo(TARGET_OPPONENT);
            assertThat(rebound.action()).isEqualTo(new Effect.Draw(TARGET_OPPONENT, Amount.exact(1)));
        }
    }
}
