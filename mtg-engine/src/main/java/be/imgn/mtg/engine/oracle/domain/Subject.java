package be.imgn.mtg.engine.oracle.domain;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// The subject of an effect — what it operates on.
public sealed interface Subject {
    record Select(Selector selector) implements Subject {}

    /// Pronoun or demonstrative phrase used as a subject back-reference.
    /// `that` is an optional restrictive clause (Blow Your House Down:
    /// "Destroy any of them that are Walls.").
    record Pronoun(PronounType type, Selector.@Nullable ThatClause that) implements Subject {
        public Pronoun(PronounType type) {
            this(type, null);
        }

        public Pronoun withThat(Selector.ThatClause that) {
            return new Pronoun(type, that);
        }
    }

    record Demonstrative(String determiner, String type) implements Subject {}

    /// "any target" (603.11 variant used by damage effects). `other` is
    /// set when the oracle text says "any *other* target" — a
    /// distinctness constraint against a prior target in the same
    /// effect (e.g., Arc Trail). `that` carries an optional restrictive
    /// clause — Needle Drop: "any target that was dealt damage this
    /// turn" — as a structured [Selector.ThatClause].
    record AnyTarget(boolean other, Selector.@Nullable ThatClause that) implements Subject {
        public AnyTarget(boolean other) {
            this(other, null);
        }

        public AnyTarget asOther() {
            return new AnyTarget(true, that);
        }

        public AnyTarget withThat(Selector.ThatClause that) {
            return new AnyTarget(other, that);
        }
    }

    record Player(PlayerRef ref) implements Subject {}

    /// "\[player-ref\] \[participial-clause\]" — a player reference
    /// narrowed by a resolution-history participle (Wicked Akuba:
    /// "target player dealt damage by this creature this turn"). The
    /// participle rides on a structured [Selector.ThatClause], not a
    /// free string.
    record PlayerWithParticiple(PlayerRef ref, Selector.ThatClause participle) implements Subject {}

    record SelfRef(@Nullable String type) implements Subject {}

    record PossessiveSubject(String possessive, String role) implements Subject {}

    /// "The \[ordinal|next\] \[type \[or type\]\]? spell \[you cast this turn|
    /// you cast each turn|of a turn\]" — a positional spell reference
    /// (Insist, Overmaster, Hardened Berserker, Uthros Psionicist,
    /// Nullstone Gargoyle). `position` names the slot, `types` narrows
    /// to specific card types (empty list for "any spell"), `window`
    /// names the binding window. Replaces the string-based
    /// PossessiveSubject encoding for this family.
    record PositionalSpell(Position position, List<CardType> types, Window window) implements Subject {
        public enum Position {
            FIRST,
            SECOND,
            THIRD,
            FOURTH,
            /// "The next spell …" — relative to the cast moment.
            NEXT
        }

        public enum Window {
            /// "you cast this turn" — bound to the controller within
            /// the current turn (Insist, Overmaster, Hardened Berserker).
            YOU_CAST_THIS_TURN,
            /// "you cast each turn" — recurring per-turn binding
            /// (Uthros Psionicist).
            YOU_CAST_EACH_TURN,
            /// "of a turn" — player-agnostic ordinal across the turn
            /// (Nullstone Gargoyle).
            OF_A_TURN
        }
    }

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
    /// optional `type` names the plural target type (`"creatures"`,
    /// `"lands"`, …); `null` for the bare `targets` form.
    record EachOfTargets(Amount count, @Nullable String type) implements Subject {
        public EachOfTargets(Amount count) {
            this(count, null);
        }
    }

    /// Creates a [Player] subject from a player reference.
    static Subject player(PlayerRef ref) {
        return new Player(ref);
    }

    /// Creates a [Select] subject from a selector.
    static Subject select(Selector sel) {
        return new Select(sel);
    }

    /// Creates a [Pronoun] subject.
    static Pronoun pronoun(PronounType type) {
        return new Pronoun(type);
    }

    /// Creates a [SelfRef] subject.
    static Subject selfRef(@Nullable String type) {
        return new SelfRef(type);
    }

    /// Returns a plain "any target" subject. Use
    /// [AnyTarget#asOther()] for the "any other target" variant.
    static Subject anyTarget() {
        return new AnyTarget(false);
    }

    /// Creates a [Demonstrative] subject.
    static Subject demonstrative(String determiner, String type) {
        return new Demonstrative(determiner, type);
    }

    /// Creates a [PossessiveSubject] subject.
    static Subject possessiveSubject(String possessive, String role) {
        return new PossessiveSubject(possessive, role);
    }

    /// Closed set of player references that appear in oracle text.
    enum PlayerRef {
        YOU,
        TARGET_PLAYER,
        EACH_PLAYER,
        ANY_PLAYER,
        /// "A player" — existentially-quantified player, typically used as
        /// a trigger subject ("Whenever a player casts a spell").
        A_PLAYER,
        TARGET_OPPONENT,
        EACH_OPPONENT,
        /// "An opponent" — existentially-quantified opponent, typically a
        /// trigger subject ("Whenever an opponent loses life").
        AN_OPPONENT,
        THAT_PLAYER,
        /// "That opponent" — back-reference to an opponent named earlier
        /// in the same clause (Zhang Liao: "Whenever ~ deals damage to
        /// an opponent, that opponent discards a card.").
        THAT_OPPONENT,
        DEFENDING_PLAYER,
        THEY,
        /// "Your opponents" — all opponents collectively (rule 102.2).
        YOUR_OPPONENTS,
        /// "Each other player" — every player except the controller
        /// (includes teammates in multiplayer; distinct from
        /// [#EACH_OPPONENT]).
        EACH_OTHER_PLAYER,
        /// "Any number of opponents" — chooser-selected subset of
        /// opponents (Windgrace's Judgment: "For any number of
        /// opponents, destroy target nonland permanent that player
        /// controls."). Distinct from [#EACH_OPPONENT] (all
        /// opponents) and [#AN_OPPONENT] (existential).
        ANY_NUMBER_OF_OPPONENTS,
        /// "Enchanted player" — the player enchanted by an Aura
        /// (rule 303.4i). Used by player-targeting Curse Auras
        /// (Curse of the Bloody Tome).
        ENCHANTED_PLAYER,
        /// "The chosen player" — back-reference to a player named by a
        /// preceding [be.imgn.mtg.engine.oracle.domain.Effect.Choose]
        /// effect (Cursed Rack: "As this artifact enters, choose an
        /// opponent. The chosen player's maximum hand size is four.").
        CHOSEN_PLAYER,
        /// "The chosen opponent" — back-reference variant for the
        /// opponent-typed choose form.
        CHOSEN_OPPONENT
    }
}
