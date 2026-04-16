package be.imgn.mtg.engine.oracle;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// Composable object selector: [quantifier] [qualifier]* type [with]* [zone] [controller].
public record Selector(
        Quantifier quantifier,
        List<Qualifier> qualifiers,
        TypeExpression type,
        List<WithClause> withClauses,
        @Nullable ControllerClause controller) {

    Selector(Quantifier quantifier, TypeExpression type) {
        this(quantifier, List.of(), type, List.of(), null);
    }

    Selector(Quantifier quantifier, List<Qualifier> qualifiers, TypeExpression type) {
        this(quantifier, qualifiers, type, List.of(), null);
    }

    Selector(TypeExpression type) {
        this(Quantifier.one(), List.of(), type, List.of(), null);
    }

    Selector(List<Qualifier> qualifiers, TypeExpression type) {
        this(Quantifier.one(), qualifiers, type, List.of(), null);
    }

    Selector withWithClause(WithClause wc) {
        return new Selector(quantifier, qualifiers, type, List.of(wc), controller);
    }

    Selector withController(ControllerClause cc) {
        return new Selector(quantifier, qualifiers, type, withClauses, cc);
    }

    public sealed interface Quantifier {
        record One() implements Quantifier {}

        record All() implements Quantifier {}

        record Each() implements Quantifier {}

        record Every() implements Quantifier {}

        record Another() implements Quantifier {}

        record Other() implements Quantifier {}

        record Count(int n) implements Quantifier {}

        record UpTo(int n) implements Quantifier {}

        record AnyNumber() implements Quantifier {}

        record The() implements Quantifier {}

        record Variable() implements Quantifier {}

        /// Creates a {@link One} quantifier.
        static Quantifier one() {
            return new One();
        }

        /// Creates an {@link All} quantifier.
        static Quantifier all() {
            return new All();
        }

        /// Creates an {@link Each} quantifier.
        static Quantifier each() {
            return new Each();
        }

        /// Creates an {@link Every} quantifier.
        static Quantifier every() {
            return new Every();
        }

        /// Creates an {@link Another} quantifier.
        static Quantifier another() {
            return new Another();
        }

        /// Creates an {@link Other} quantifier.
        static Quantifier other() {
            return new Other();
        }

        /// Creates a {@link Count} quantifier.
        static Quantifier count(int n) {
            return new Count(n);
        }

        /// Creates an {@link UpTo} quantifier.
        static Quantifier upTo(int n) {
            return new UpTo(n);
        }

        /// Creates an {@link AnyNumber} quantifier.
        static Quantifier anyNumber() {
            return new AnyNumber();
        }

        /// Creates a {@link The} quantifier.
        static Quantifier the() {
            return new The();
        }

        /// Creates a {@link Variable} quantifier.
        static Quantifier variable() {
            return new Variable();
        }
    }

    public sealed interface Qualifier {
        record Target() implements Qualifier {}

        record Color(ColorFilter filter) implements Qualifier {}

        record OfSupertype(Supertype supertype) implements Qualifier {}

        record NegatedSupertype(Supertype supertype) implements Qualifier {}

        record NegatedCardType(CardType type) implements Qualifier {}

        record NegatedSubtype(String name) implements Qualifier {}

        record Status(String status) implements Qualifier {}

        record CombatStatus(String status) implements Qualifier {}

        record Historic() implements Qualifier {}

        record Outlaw() implements Qualifier {}

        record NegatedOutlaw() implements Qualifier {}

        record IsToken() implements Qualifier {}

        record NonToken() implements Qualifier {}

        record OtherQ() implements Qualifier {}

        // Singleton instances for no-arg qualifiers
        Qualifier TARGET = new Target();
        Qualifier HISTORIC = new Historic();
        Qualifier OUTLAW = new Outlaw();
        Qualifier NEGATED_OUTLAW = new NegatedOutlaw();
        Qualifier IS_TOKEN = new IsToken();
        Qualifier NON_TOKEN = new NonToken();
        Qualifier OTHER = new OtherQ();

        // Factory methods for parameterized qualifiers
        static Qualifier color(ColorFilter filter) {
            return new Color(filter);
        }

        static Qualifier ofSupertype(Supertype supertype) {
            return new OfSupertype(supertype);
        }

        static Qualifier negatedSupertype(Supertype supertype) {
            return new NegatedSupertype(supertype);
        }

        static Qualifier negatedCardType(CardType type) {
            return new NegatedCardType(type);
        }

        static Qualifier negatedSubtype(String name) {
            return new NegatedSubtype(name);
        }

        static Qualifier status(String status) {
            return new Status(status);
        }

        static Qualifier combatStatus(String status) {
            return new CombatStatus(status);
        }
    }

    public sealed interface TypeExpression {
        record Single(SingleType type) implements TypeExpression {}

        record Or(List<SingleType> types) implements TypeExpression {}

        record Compound(List<SingleType> types) implements TypeExpression {}

        /// Creates a {@link Single} type expression.
        static TypeExpression single(SingleType type) {
            return new Single(type);
        }

        /// Creates an {@link Or} type expression.
        static TypeExpression or(List<SingleType> types) {
            return new Or(types);
        }

        /// Creates a {@link Compound} type expression.
        static TypeExpression compound(List<SingleType> types) {
            return new Compound(types);
        }
    }

    public sealed interface SingleType {
        record OfGameObject(GameObjectType type) implements SingleType {}

        record OfCard(CardType type) implements SingleType {}

        record OfSubtype(String name) implements SingleType {}

        record ObjectCard(GameObjectType object, CardType card) implements SingleType {}

        /// Creates an {@link OfGameObject} single type.
        static SingleType ofGameObject(GameObjectType type) {
            return new OfGameObject(type);
        }

        /// Creates an {@link OfCard} single type.
        static SingleType ofCard(CardType type) {
            return new OfCard(type);
        }

        /// Creates an {@link OfSubtype} single type.
        static SingleType ofSubtype(String name) {
            return new OfSubtype(name);
        }

        /// Creates an {@link ObjectCard} single type.
        static SingleType objectCard(GameObjectType object, CardType card) {
            return new ObjectCard(object, card);
        }
    }

    public record WithClause(boolean negated, String predicate) {}

    public record ControllerClause(String description) {}
}
