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

    /// "any target" / "another target" / "a third target" (603.11 variant
    /// used by damage effects). `ordinal` encodes the positional slot:
    /// 1 = first ("any target"), 2 = second ("another target" / "any other
    /// target"), 3 = third ("a third target" — Cone of Flame). `that` carries
    /// an optional restrictive clause — Needle Drop: "any target that was
    /// dealt damage this turn" — as a structured [Selector.ThatClause].
    /// `chooser` is set when targeting is delegated — Cuombajj Witches:
    /// "any target of an opponent's choice" — capturing who selects the target.
    record AnyTarget(
            int ordinal,
            Selector.@Nullable ThatClause that,
            @Nullable PlayerRef chooser) implements Subject {
        public AnyTarget(int ordinal) {
            this(ordinal, null, null);
        }

        /// Returns the "any other target" variant (ordinal 2).
        public AnyTarget asOther() {
            return new AnyTarget(2, that, chooser);
        }

        /// Returns the "a third target" variant (ordinal 3).
        public AnyTarget asThird() {
            return new AnyTarget(3, that, chooser);
        }

        public AnyTarget withThat(Selector.ThatClause that) {
            return new AnyTarget(ordinal, that, chooser);
        }

        public AnyTarget withChooser(PlayerRef chooser) {
            return new AnyTarget(ordinal, that, chooser);
        }
    }

    record Player(PlayerRef ref) implements Subject {}

    /// "the player or planeswalker it's attacking" — back-reference
    /// to the current attack-target of the creature (Scorch Spitter:
    /// "Whenever this creature attacks, it deals 1 damage to the
    /// player or planeswalker it's attacking."). Singleton because
    /// the referent is bound by combat state, not by the oracle text.
    enum AttackedByIt implements Subject {
        ATTACKED_BY_IT
    }

    /// "\[player-ref\] \[participial-clause\]" — a player reference
    /// narrowed by a resolution-history participle (Wicked Akuba:
    /// "target player dealt damage by this creature this turn"). The
    /// participle rides on a structured [Selector.ThatClause], not a
    /// free string.
    record PlayerWithParticiple(PlayerRef ref, Selector.ThatClause participle) implements Subject {}

    record SelfRef(@Nullable String type) implements Subject {}

    record PossessiveSubject(String possessive, String role) implements Subject {}

    /// "\[player\]'s opponents" — the set of opponents of a referenced
    /// player (Heartwood Storyteller: "each of that player's opponents
    /// may draw a card."). Distinct from [PlayerRef#YOUR_OPPONENTS]
    /// which is collective opponents of `you`; this variant binds to an
    /// arbitrary referenced player.
    record OpponentsOf(Subject of) implements Subject {}

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

    /// "The next \[type\]? card you play this turn" / "The first card you
    /// draw each turn" — positional card reference. Cards include lands
    /// (played, not cast) so this is structurally distinct from
    /// [PositionalSpell] (rule 601 cast vs rule 305 play for lands).
    /// Reuses [PositionalSpell.Position]. `window` indicates the action
    /// that produces the positional card.
    record PositionalCard(PositionalSpell.Position position, List<CardType> types, Window window) implements Subject {

        /// The action/timing context that produces the positional card.
        public enum Window {
            /// "you play this turn" — the next card played (Scout's Warning).
            YOU_PLAY_THIS_TURN,
            /// "you draw each turn" — the nth card drawn per turn
            /// (Primitive Etchings: "the first card you draw each turn").
            YOU_DRAW_EACH_TURN,
            /// "drafted from this booster pack" — the next card picked
            /// during a Conspiracy draft (Cogwork Spy).
            DRAFTED_FROM_BOOSTER
        }

        /// Backward-compatible two-arg constructor; defaults to
        /// [Window#YOU_PLAY_THIS_TURN].
        public PositionalCard(PositionalSpell.Position position, List<CardType> types) {
            this(position, types, Window.YOU_PLAY_THIS_TURN);
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

    /// "each of [count] target(s) [selector]?" — split-target expression
    /// where the effect is applied once per chosen target (e.g., Meteor
    /// Blast: "each of X targets"; Thrive: "each of X target creatures";
    /// Vineshaper Mystic: "each of up to two target Merfolk you control").
    /// The optional `selector` narrows the eligible targets to a typed
    /// subset; `null` for the bare `targets` form.
    record EachOfTargets(Amount count, @Nullable Selector selector) implements Subject {
        public EachOfTargets(Amount count) {
            this(count, null);
        }
    }

    /// "half \[selector\] \[, rounded up|down\]?" — a subset whose
    /// cardinality is half the matched group, rounded as specified
    /// (Split the Party: "Return half the creatures they control to
    /// their owner's hand, rounded up."). `rounding` is `null` until
    /// an inline ", rounded up/down" tail attaches it.
    record HalfOf(Selector selector, Amount.@Nullable Rounding rounding) implements Subject {
        public HalfOf(Selector selector) {
            this(selector, null);
        }

        public HalfOf withRounding(Amount.Rounding rounding) {
            return new HalfOf(selector, rounding);
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

    /// Returns a plain "any target" subject (ordinal 1). Use
    /// [AnyTarget#asOther()] for "any other target" (ordinal 2) and
    /// [AnyTarget#asThird()] for "a third target" (ordinal 3).
    static Subject anyTarget() {
        return new AnyTarget(1);
    }

    /// Creates a [Demonstrative] subject.
    static Subject demonstrative(String determiner, String type) {
        return new Demonstrative(determiner, type);
    }

    /// Creates a [PossessiveSubject] subject.
    static Subject possessiveSubject(String possessive, String role) {
        return new PossessiveSubject(possessive, role);
    }
}
