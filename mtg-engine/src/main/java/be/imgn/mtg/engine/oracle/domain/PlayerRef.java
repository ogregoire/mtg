package be.imgn.mtg.engine.oracle.domain;

import java.util.Locale;

/// A player reference in oracle text. Unified across the subject
/// position ([Subject.Player], [Subject.PlayerWithParticiple]) and
/// the controller clause's left-hand side
/// ([Selector.ControllerClause.Body.Controls],
/// [Selector.ControllerClause.Body.Owns],
/// [Selector.ControllerClause.Body.Casts],
/// [Selector.ControllerClause.Body.Discarded],
/// [Selector.ControllerClause.Body.Attacking]).
///
/// Two cases:
///
/// - [Pronoun] for atomic forms — pronouns, anaphors, quantifier
///   pronouns, back-references. No inner structure.
/// - [Qualified] for "\<qualifier\> \<base\>" forms — a base
///   "player"/"opponent" plus a game-state qualifier (TARGET,
///   DEFENDING, ENCHANTED, ATTACKING).
public sealed interface PlayerRef {

    /// Display string for use in possessive constructions ("target
    /// player's hand", "your team's library"). Lowercase, spaces
    /// instead of underscores.
    default String displayName() {
        return switch (this) {
            case Pronoun p -> p.name().toLowerCase(Locale.ROOT).replace('_', ' ');
            case Qualified q ->
                q.qualifier().name().toLowerCase(Locale.ROOT) + " "
                        + q.base().name().toLowerCase(Locale.ROOT);
        };
    }

    // ── Convenience factories for the common qualifier+base combinations ──

    static Qualified targetPlayer() {
        return new Qualified(Qualified.Qualifier.TARGET, Qualified.Base.PLAYER);
    }

    static Qualified targetOpponent() {
        return new Qualified(Qualified.Qualifier.TARGET, Qualified.Base.OPPONENT);
    }

    static Qualified defendingPlayer() {
        return new Qualified(Qualified.Qualifier.DEFENDING, Qualified.Base.PLAYER);
    }

    static Qualified enchantedPlayer() {
        return new Qualified(Qualified.Qualifier.ENCHANTED, Qualified.Base.PLAYER);
    }

    static Qualified enchantedOpponent() {
        return new Qualified(Qualified.Qualifier.ENCHANTED, Qualified.Base.OPPONENT);
    }

    static Qualified attackingPlayer() {
        return new Qualified(Qualified.Qualifier.ATTACKING, Qualified.Base.PLAYER);
    }

    /// Atomic player references. The union of the pre-merge atomic
    /// vocabularies of `Subject.PlayerRef` and
    /// `Selector.ControllerClause.Who`.
    enum Pronoun implements PlayerRef {
        /// "You" — the controller of the spell or ability.
        YOU,
        /// "They" — anaphoric reference to a previously named
        /// player.
        THEY,
        /// "Your opponents" — all opponents collectively (rule 102.2).
        YOUR_OPPONENTS,
        /// "Your team" — multiplayer team-format collective
        /// (Two-Headed Giant). The controller plus their teammates
        /// as a single player-group.
        YOUR_TEAM,

        // ── Existential / quantifier pronouns ──

        /// "A player" — existentially-quantified player, typically
        /// a trigger subject ("Whenever a player casts a spell").
        A_PLAYER,
        /// "An opponent" — existentially-quantified opponent
        /// ("Whenever an opponent loses life").
        AN_OPPONENT,
        /// "Any player" — universally-quantified player.
        ANY_PLAYER,
        /// "Each player".
        EACH_PLAYER,
        /// "Each opponent".
        EACH_OPPONENT,
        /// "Each other player" — every player except the
        /// controller; in multiplayer includes teammates (distinct
        /// from [#EACH_OPPONENT]).
        EACH_OTHER_PLAYER,
        /// "Any number of players" — chooser-selected subset of all
        /// players (Reverse the Sands).
        ANY_NUMBER_OF_PLAYERS,
        /// "Any number of opponents" — chooser-selected subset of
        /// opponents (Windgrace's Judgment).
        ANY_NUMBER_OF_OPPONENTS,

        // ── Anaphoric demonstratives ──

        /// "That player" — back-reference to a player named earlier
        /// in the same clause.
        THAT_PLAYER,
        /// "Those players" — plural back-reference to a set of
        /// players named in the same resolution (Skull Rend).
        THOSE_PLAYERS,
        /// "That opponent" — back-reference to an opponent named
        /// earlier in the same clause (Zhang Liao).
        THAT_OPPONENT,

        // ── Choose / refer back-references ──

        /// "The chosen player" — back-reference to a player named
        /// by a preceding [Effect.Choose] (Cursed Rack).
        CHOSEN_PLAYER,
        /// "The chosen opponent" — opponent-typed back-reference.
        CHOSEN_OPPONENT,

        // ── Source-relative ──

        /// "Its controller" — the controller of the object whose
        /// ability is resolving (Tectonic Instability: "tap all
        /// lands its controller controls.").
        ITS_CONTROLLER
    }

    /// "\<qualifier\> \<base\>" — game-state qualifier on a base
    /// "player"/"opponent". Examples:
    /// - "target player" → Qualified(TARGET, PLAYER)
    /// - "target opponent" → Qualified(TARGET, OPPONENT)
    /// - "defending player" → Qualified(DEFENDING, PLAYER) — Fiend Binder
    /// - "enchanted player" → Qualified(ENCHANTED, PLAYER) — Curse of Death's Hold
    /// - "enchanted opponent" → Qualified(ENCHANTED, OPPONENT) — Psychic Possession
    /// - "attacking player" → Qualified(ATTACKING, PLAYER) — Souls of the Faultless, Mogg Toady
    record Qualified(Qualifier qualifier, Base base) implements PlayerRef {
        public enum Qualifier {
            TARGET,
            DEFENDING,
            ENCHANTED,
            ATTACKING
        }

        public enum Base {
            PLAYER,
            OPPONENT
        }
    }
}
