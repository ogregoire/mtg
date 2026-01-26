package be.imgn.mtg.engine.characteristics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CounterTypeTest {

    @Test
    void ofReturnsStandardCounterTypeWhenMatching() {
        var counterType = CounterType.of("+1/+1");

        assertThat(counterType).isEqualTo(StandardCounterType.PLUS_ONE_PLUS_ONE);
    }

    @Test
    void ofReturnsStandardCounterTypeIgnoringCase() {
        var counterType = CounterType.of("LOYALTY");

        assertThat(counterType).isEqualTo(StandardCounterType.LOYALTY);
    }

    @Test
    void ofReturnsCustomCounterTypeWhenNotMatching() {
        var counterType = CounterType.of("my-custom-counter");

        assertThat(counterType).isInstanceOf(CustomCounterType.class);
        assertThat(counterType.text()).isEqualTo("my-custom-counter");
    }
}
