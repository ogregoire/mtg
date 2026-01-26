package be.imgn.mtg.engine.ability.internal.parser.selector;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Quantifier")
class QuantifierTest {

    @Test
    @DisplayName("One quantifier can be created")
    void oneCreation() {
        var quantifier = new Quantifier.One();
        assertThat(quantifier).isNotNull();
    }

    @Test
    @DisplayName("Count quantifier stores value")
    void countValue() {
        var quantifier = new Quantifier.Count(3);
        assertThat(quantifier.value()).isEqualTo(3);
    }

    @Test
    @DisplayName("All quantifier can be created")
    void allCreation() {
        var quantifier = new Quantifier.All();
        assertThat(quantifier).isNotNull();
    }

    @Test
    @DisplayName("Each quantifier can be created")
    void eachCreation() {
        var quantifier = new Quantifier.Each();
        assertThat(quantifier).isNotNull();
    }

    @Test
    @DisplayName("UpTo quantifier stores max value")
    void upToValue() {
        var quantifier = new Quantifier.UpTo(5);
        assertThat(quantifier.max()).isEqualTo(5);
    }

    @Test
    @DisplayName("Any quantifier can be created")
    void anyCreation() {
        var quantifier = new Quantifier.Any();
        assertThat(quantifier).isNotNull();
    }

    @Test
    @DisplayName("Another quantifier can be created")
    void anotherCreation() {
        var quantifier = new Quantifier.Another();
        assertThat(quantifier).isNotNull();
    }
}
