package be.imgn.mtg.engine.cost.internal;

import static be.imgn.mtg.engine.assertions.MTGAssertions.assertThat;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.engine.cost.Costs;

class CostsTest {

    @Test
    void emptyCosts() {
        var costs = Costs.empty();

        assertThat(costs).isEmpty().hasCount(0);
    }

    @Test
    void singleCost() {
        var cost = new TapCost();
        var costs = Costs.of(cost);

        assertThat(costs).isNotEmpty().hasCount(1).contains(cost);
    }

    @Test
    void multipleCosts() {
        var cost1 = new TapCost();
        var cost2 = new LoyaltyCost(-3);

        var costs = Costs.of(cost1, cost2);

        assertThat(costs).hasCount(2).contains(cost1).contains(cost2);
    }

    @Test
    void orderIsPreserved() {
        var cost1 = new TapCost();
        var cost2 = new LoyaltyCost(-2);
        var cost3 = new UntapCost();

        var costs = Costs.of(cost1, cost2, cost3);

        assertThat(costs).containsExactlyInOrder(cost1, cost2, cost3);
    }

    @Test
    void builderPreservesOrder() {
        var cost1 = new TapCost();
        var cost2 = new LoyaltyCost(-1);
        var cost3 = new UntapCost();

        var costs = Costs.builder().add(cost1).add(cost2).add(cost3).build();

        assertThat(costs).containsExactlyInOrder(cost1, cost2, cost3);
    }

    @Test
    void toBuilderPreservesOrder() {
        var cost1 = new TapCost();
        var cost2 = new LoyaltyCost(-4);

        var original = Costs.of(cost1, cost2);
        var cost3 = new UntapCost();
        var modified = original.toBuilder().add(cost3).build();

        assertThat(modified).containsExactlyInOrder(cost1, cost2, cost3);
    }

    @Test
    void duplicateCostsAreIgnored() {
        var cost = new TapCost();
        var costs = Costs.of(cost, cost, cost);

        assertThat(costs).hasCount(1);
    }
}
