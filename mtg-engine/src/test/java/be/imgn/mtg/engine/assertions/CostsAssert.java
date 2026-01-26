package be.imgn.mtg.engine.assertions;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;

import be.imgn.mtg.engine.cost.Cost;
import be.imgn.mtg.engine.cost.Costs;

/// Assertion class for Costs.
public class CostsAssert extends AbstractObjectAssert<CostsAssert, Costs> {

    protected CostsAssert(Costs actual) {
        super(actual, CostsAssert.class);
    }

    public static CostsAssert assertThat(Costs actual) {
        return new CostsAssert(actual);
    }

    public CostsAssert isEmpty() {
        isNotNull();
        if (!actual.isEmpty()) {
            failWithMessage("Expected costs to be empty but was <%s>", actual);
        }
        return this;
    }

    public CostsAssert isNotEmpty() {
        isNotNull();
        if (actual.isEmpty()) {
            failWithMessage("Expected costs not to be empty");
        }
        return this;
    }

    public CostsAssert hasCount(int expected) {
        isNotNull();
        if (actual.count() != expected) {
            failWithMessage("Expected cost count to be <%d> but was <%d>", expected, actual.count());
        }
        return this;
    }

    public CostsAssert contains(Cost cost) {
        isNotNull();
        if (!actual.contains(cost)) {
            failWithMessage("Expected costs to contain <%s> but was <%s>", cost, actual);
        }
        return this;
    }

    public CostsAssert doesNotContain(Cost cost) {
        isNotNull();
        if (actual.contains(cost)) {
            failWithMessage("Expected costs not to contain <%s> but was <%s>", cost, actual);
        }
        return this;
    }

    public CostsAssert containsExactlyInOrder(Cost... costs) {
        isNotNull();
        List<Cost> actualList = new ArrayList<>();
        actual.forEach(actualList::add);

        if (actualList.size() != costs.length) {
            failWithMessage("Expected exactly <%d> costs but had <%d>: <%s>", costs.length, actualList.size(), actual);
        }

        for (var i = 0; i < costs.length; i++) {
            if (!actualList.get(i).equals(costs[i])) {
                failWithMessage("Expected cost at index <%d> to be <%s> but was <%s>", i, costs[i], actualList.get(i));
            }
        }
        return this;
    }
}
