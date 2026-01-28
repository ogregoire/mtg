package be.imgn.mtg.engine.cost;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.ability.internal.parser.selector.Amount;
import be.imgn.mtg.engine.cost.internal.LifeCost;
import be.imgn.mtg.engine.cost.internal.TapCost;
import be.imgn.mtg.engine.cost.internal.UntapCost;

class CostsTest {

    @Test
    void emptyCosts() {
        var costs = Costs.empty();

        assertThat(costs.isEmpty()).isTrue();
        assertThat(costs.count()).isEqualTo(0);
    }

    @Test
    void singleCost() {
        var cost = new TapCost();
        var costs = Costs.of(cost);

        assertThat(costs.isEmpty()).isFalse();
        assertThat(costs.count()).isEqualTo(1);
        assertThat(costs.contains(cost)).isTrue();
    }

    @Test
    void multipleCosts() {
        var tap = new TapCost();
        var life = new LifeCost(new Amount.Exact(2));
        var costs = Costs.of(tap, life);

        assertThat(costs.count()).isEqualTo(2);
        assertThat(costs.contains(tap)).isTrue();
        assertThat(costs.contains(life)).isTrue();
    }

    @Test
    void builderAddsCosts() {
        var tap = new TapCost();
        var life = new LifeCost(new Amount.Exact(3));
        var costs = Costs.builder().add(tap).add(life).build();

        assertThat(costs.count()).isEqualTo(2);
    }

    @Test
    void toBuilderCopiesCosts() {
        var tap = new TapCost();
        var life = new LifeCost(new Amount.Exact(1));
        var original = Costs.of(tap);
        var modified = original.toBuilder().add(life).build();

        assertThat(original.count()).isEqualTo(1);
        assertThat(modified.count()).isEqualTo(2);
    }

    @Nested
    @DisplayName("toCosts() collector")
    class ToCostsCollector {

        @Test
        void collectsEmptyStream() {
            var costs = Stream.<Cost>empty().collect(Costs.toCosts());

            assertThat(costs.isEmpty()).isTrue();
        }

        @Test
        void collectsSingleElement() {
            var tap = new TapCost();
            var costs = Stream.of(tap).collect(Costs.toCosts());

            assertThat(costs.count()).isEqualTo(1);
            assertThat(costs.contains(tap)).isTrue();
        }

        @Test
        void collectsMultipleElements() {
            var tap = new TapCost();
            var life = new LifeCost(new Amount.Exact(2));
            var untap = new UntapCost();
            var costs = Stream.of(tap, life, untap).collect(Costs.toCosts());

            assertThat(costs.count()).isEqualTo(3);
            assertThat(costs.contains(tap)).isTrue();
            assertThat(costs.contains(life)).isTrue();
            assertThat(costs.contains(untap)).isTrue();
        }

        @Test
        void deduplicatesDuplicates() {
            var tap = new TapCost();
            var costs = Stream.of(tap, tap, tap).collect(Costs.toCosts());

            assertThat(costs.count()).isEqualTo(1);
        }

        @Test
        void worksWithParallelStream() {
            var tap = new TapCost();
            var life1 = new LifeCost(new Amount.Exact(1));
            var life2 = new LifeCost(new Amount.Exact(2));
            var life3 = new LifeCost(new Amount.Exact(3));
            var untap = new UntapCost();
            var costs = Stream.of(tap, life1, life2, life3, untap).parallel().collect(Costs.toCosts());

            assertThat(costs.count()).isEqualTo(5);
        }
    }
}
