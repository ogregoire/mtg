package be.imgn.mtg.engine.oracle;

import org.jspecify.annotations.Nullable;

/// The subject of an effect — what it operates on.
public sealed interface Subject {
    record Select(Selector selector) implements Subject {}

    record Pronoun(String type) implements Subject {}

    record Demonstrative(String determiner, String type) implements Subject {}

    record AnyTarget() implements Subject {}

    record Player(PlayerRef ref) implements Subject {}

    record SelfRef(@Nullable String type) implements Subject {}

    record PossessiveSubject(String possessive, String role) implements Subject {}

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

    /// Creates an {@link AnyTarget} subject.
    static Subject anyTarget() {
        return new AnyTarget();
    }

    /// Creates a {@link Demonstrative} subject.
    static Subject demonstrative(String determiner, String type) {
        return new Demonstrative(determiner, type);
    }

    /// Creates a {@link PossessiveSubject} subject.
    static Subject possessiveSubject(String possessive, String role) {
        return new PossessiveSubject(possessive, role);
    }

    sealed interface PlayerRef {
        record You() implements PlayerRef {}

        record TargetPlayer() implements PlayerRef {}

        record EachPlayer() implements PlayerRef {}

        record AnyPlayer() implements PlayerRef {}

        record TargetOpponent() implements PlayerRef {}

        record EachOpponent() implements PlayerRef {}

        record ThatPlayer() implements PlayerRef {}

        record DefendingPlayer() implements PlayerRef {}

        record They() implements PlayerRef {}

        /// Creates a {@link You} player reference.
        static PlayerRef you() {
            return new You();
        }

        /// Creates a {@link TargetPlayer} player reference.
        static PlayerRef targetPlayer() {
            return new TargetPlayer();
        }

        /// Creates an {@link EachPlayer} player reference.
        static PlayerRef eachPlayer() {
            return new EachPlayer();
        }

        /// Creates an {@link AnyPlayer} player reference.
        static PlayerRef anyPlayer() {
            return new AnyPlayer();
        }

        /// Creates a {@link TargetOpponent} player reference.
        static PlayerRef targetOpponent() {
            return new TargetOpponent();
        }

        /// Creates an {@link EachOpponent} player reference.
        static PlayerRef eachOpponent() {
            return new EachOpponent();
        }

        /// Creates a {@link ThatPlayer} player reference.
        static PlayerRef thatPlayer() {
            return new ThatPlayer();
        }

        /// Creates a {@link DefendingPlayer} player reference.
        static PlayerRef defendingPlayer() {
            return new DefendingPlayer();
        }

        /// Creates a {@link They} player reference.
        static PlayerRef they() {
            return new They();
        }
    }
}
