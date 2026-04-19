package be.imgn.mtg.engine.oracle;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// The subject of an effect — what it operates on.
public sealed interface Subject {
    record Select(Selector selector) implements Subject {}

    record Pronoun(String type) implements Subject {}

    record Demonstrative(String determiner, String type) implements Subject {}

    enum AnyTarget implements Subject {
        ANY_TARGET
    }

    record Player(PlayerRef ref) implements Subject {}

    record SelfRef(@Nullable String type) implements Subject {}

    record PossessiveSubject(String possessive, String role) implements Subject {}

    /// Two-or-more subjects joined by "and". The conjunction denotes that the
    /// effect operates on every part simultaneously — e.g., "Destroy target
    /// creature and target land" or "Exile ~ and target permanent".
    record Multiple(List<Subject> parts) implements Subject {}

    /// Two-or-more subjects joined by "or" — a single target whose type must
    /// match any one of the alternatives (e.g., Stream of Acid: "Destroy
    /// target land or nonblack creature").
    record OneOf(List<Subject> alternatives) implements Subject {}

    /// "each of [count] target(s) [type]?" — split-target expression where
    /// the effect is applied once per chosen target (e.g., Meteor Blast:
    /// "each of X targets"; Thrive: "each of X target creatures"). The
    /// optional {@code type} names the plural target type (`"creatures"`,
    /// `"lands"`, …); {@code null} for the bare `targets` form.
    record EachOfTargets(Amount count, @Nullable String type) implements Subject {
        EachOfTargets(Amount count) {
            this(count, null);
        }
    }

    /// Creates a {@link Player} subject from a player reference.
    static Subject player(PlayerRef ref) {
        return new Player(ref);
    }

    /// Creates a {@link Select} subject from a selector.
    static Subject select(Selector sel) {
        return new Select(sel);
    }

    /// Creates a {@link Pronoun} subject.
    static Subject pronoun(String type) {
        return new Pronoun(type);
    }

    /// Creates a {@link SelfRef} subject.
    static Subject selfRef(@Nullable String type) {
        return new SelfRef(type);
    }

    /// Returns the {@link AnyTarget} singleton.
    static Subject anyTarget() {
        return AnyTarget.ANY_TARGET;
    }

    /// Creates a {@link Demonstrative} subject.
    static Subject demonstrative(String determiner, String type) {
        return new Demonstrative(determiner, type);
    }

    /// Creates a {@link PossessiveSubject} subject.
    static Subject possessiveSubject(String possessive, String role) {
        return new PossessiveSubject(possessive, role);
    }

    /// Closed set of player references that appear in oracle text.
    enum PlayerRef {
        YOU,
        TARGET_PLAYER,
        EACH_PLAYER,
        ANY_PLAYER,
        TARGET_OPPONENT,
        EACH_OPPONENT,
        THAT_PLAYER,
        DEFENDING_PLAYER,
        THEY,
        /// "Your opponents" — all opponents collectively (rule 102.2).
        YOUR_OPPONENTS
    }
}
