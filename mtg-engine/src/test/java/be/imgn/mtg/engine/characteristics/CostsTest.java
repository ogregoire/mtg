package be.imgn.mtg.engine.characteristics;

import static be.imgn.mtg.engine.assertions.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CostsTest {

    // Simple test cost implementations
    record TestCost(String name) implements Cost {
        @Override
        public boolean canPay(CostContext context) {
            return true;
        }

        @Override
        public void pay(CostContext context) {}

        @Override
        public String description() {
            return name;
        }
    }

    @Test
    void emptyCosts() {
        var costs = Costs.empty();

        assertThat(costs).isEmpty().hasCount(0);
    }

    @Test
    void singleCost() {
        var cost = new TestCost("tap");
        var costs = Costs.of(cost);

        assertThat(costs).isNotEmpty().hasCount(1).contains(cost);
    }

    @Test
    void multipleCosts() {
        var cost1 = new TestCost("tap");
        var cost2 = new TestCost("pay mana");

        var costs = Costs.of(cost1, cost2);

        assertThat(costs).hasCount(2).contains(cost1).contains(cost2);
    }

    @Test
    void orderIsPreserved() {
        var cost1 = new TestCost("first");
        var cost2 = new TestCost("second");
        var cost3 = new TestCost("third");

        var costs = Costs.of(cost1, cost2, cost3);

        assertThat(costs).containsExactlyInOrder(cost1, cost2, cost3);
    }

    @Test
    void builderPreservesOrder() {
        var cost1 = new TestCost("first");
        var cost2 = new TestCost("second");
        var cost3 = new TestCost("third");

        var costs = Costs.builder().add(cost1).add(cost2).add(cost3).build();

        assertThat(costs).containsExactlyInOrder(cost1, cost2, cost3);
    }

    @Test
    void toBuilderPreservesOrder() {
        var cost1 = new TestCost("first");
        var cost2 = new TestCost("second");

        var original = Costs.of(cost1, cost2);
        var cost3 = new TestCost("third");
        var modified = original.toBuilder().add(cost3).build();

        assertThat(modified).containsExactlyInOrder(cost1, cost2, cost3);
    }

    @Test
    void duplicateCostsAreIgnored() {
        var cost = new TestCost("same");
        var costs = Costs.of(cost, cost, cost);

        assertThat(costs).hasCount(1);
    }
}
