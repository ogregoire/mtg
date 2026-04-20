package be.imgn.mtg.engine.oracle;

import java.util.List;

import org.jspecify.annotations.Nullable;

/// A parsed ability from oracle text.
///
/// The four MTG ability types (rule 113.3) are modelled as sealed interfaces:
/// {@link Static}, {@link Triggered}, {@link Activated}, {@link Spell}.
/// "Written-out" forms have dedicated records ({@link StaticAbility},
/// {@link TriggeredAbility}, {@link ActivatedAbility}, {@link SpellAbility}).
///
/// Keyword abilities (rule 702) are *not* a fifth type — each is a shortcut
/// for an ability of one of the four types. No-parameter static keywords are
/// consolidated in {@link StaticKeyword}; no-parameter triggered keywords in
/// {@link TriggeredKeyword}. Parameterized keywords are records ({@link Ward},
/// {@link Equip}, {@link Landwalk}, {@link Protection}, …). Each directly
/// implements its underlying ability type per the rule in 702.Xa.
public sealed interface Ability {

    // ── The four ability types ────────────────────────────────────────

    sealed interface Static extends Ability {}

    sealed interface Triggered extends Ability {}

    sealed interface Activated extends Ability {}

    sealed interface Spell extends Ability {}

    // ── Written-out (non-keyword) ability forms ───────────────────────

    record StaticAbility(String text) implements Static {}

    record TriggeredAbility(
            String triggerWord,
            TriggerEvent event,
            @Nullable Condition interveningIf,
            List<Effect> effects) implements Triggered {}

    record ActivatedAbility(Cost cost, List<Effect> effects) implements Activated {}

    record SpellAbility(List<Effect> effects) implements Spell {}

    // ── Other ability structures ──────────────────────────────────────

    record Modal(String quantity, List<Mode> modes) implements Ability {}

    record Chapter(String numerals, List<Effect> effects) implements Ability {}

    record CastingModifier(String text) implements Ability {}

    record Mode(@Nullable Cost cost, List<Effect> effects) {}

    // ── Keyword abilities (rule 702) ─────────────────────────────────
    //
    // Each keyword is a shortcut for a written ability of one of the four
    // types, per rule 702.Xa. No-parameter keywords are consolidated into
    // two enums; parametrized keywords are records.

    // Static keyword abilities (rule 702) ─────────────────────────────

    /// All parameter-less static keyword abilities (rule 702). Each constant
    /// corresponds to a single keyword ability; the name uses SCREAMING_SNAKE
    /// convention matching the canonical oracle text (e.g., FIRST_STRIKE →
    /// "first strike").
    enum StaticKeyword implements Static {
        /// 702.9 — evasion.
        FLYING,
        /// 702.19 — trample combat damage.
        TRAMPLE,
        /// 702.7 — first-strike combat damage step.
        FIRST_STRIKE,
        /// 702.4 — two combat damage steps.
        DOUBLE_STRIKE,
        /// 702.10 — may attack / tap the turn it enters.
        HASTE,
        /// 702.20 — doesn't tap to attack.
        VIGILANCE,
        /// 702.17 — may block flying creatures.
        REACH,
        /// 702.111 — can only be blocked by two or more.
        MENACE,
        /// 702.13 — evasion limited to artifacts/same color.
        INTIMIDATE,
        /// 702.36 — evasion limited to artifacts/black.
        FEAR,
        /// 702.118 — can't be blocked by greater-power creatures.
        SKULK,
        /// 702.2 — destroys anything it damages.
        DEATHTOUCH,
        /// 702.15 — damage dealt also gains life.
        LIFELINK,
        /// 702.11 — can't be targeted by opponents.
        HEXPROOF,
        /// 702.18 — can't be targeted.
        SHROUD,
        /// 702.12 — can't be destroyed.
        INDESTRUCTIBLE,
        /// 702.3 — can't attack.
        DEFENDER,
        /// 702.8 — may be played any time.
        FLASH,
        /// 702.22 — combat grouping.
        BANDING,
        /// 702.26 — may phase out/in.
        PHASING,
        /// 702.31 — evasion over non-horsemanship creatures.
        HORSEMANSHIP,
        /// 702.27 — can only be blocked by creatures with shadow.
        SHADOW,
        /// 702.90 — damage becomes poison/−1 counters.
        INFECT,
        /// 702.80 — damage dealt is via −1/−1 counters.
        WITHER,
        /// 702.73 — every creature type.
        CHANGELING,
        /// 702.114 — colorless.
        DEVOID,
        /// 702.61 — can't be responded to.
        SPLIT_SECOND,
        /// 702.66 — alternative cost using exile-from-graveyard mana.
        DELVE,
        /// 702.51 — alternative cost using tapping creatures.
        CONVOKE,
        /// 702.127 — cast only from graveyard as the aftermath half.
        AFTERMATH,
        /// 702.131 — tracks the city's blessing.
        ASCEND,
        /// 702.147 — exiled zombie tokens.
        DECAYED,
        /// 702.150 — return from graveyard payment.
        COMPLEATED,
        /// 702.169 — solved-case marker.
        SOLVED,
        /// 702.161 — vehicle that's always a creature.
        LIVING_METAL,
        /// 702.163 — equip to a Legendary on ETB.
        FOR_MIRRODIN,
        /// 702.155 — lore counters add one at a time.
        READ_AHEAD,
        /// 702.89 — indestructible aura substitute.
        UMBRA_ARMOR,
        /// 702.102 — split card with fused casting.
        FUSE,
        /// 702.106 — face-down in the command zone.
        HIDDEN_AGENDA,
        /// 702.81 — cast from graveyard by discarding.
        RETRACE,
        /// 702.50 — game-ending spell.
        EPIC,
        /// 702.132 — another player may pay up to {7} of this spell's cost.
        ASSIST
    }

    /// 702.164 — deals toxic N to damaged players.
    record Toxic(int n) implements Static {}

    /// 702.16 — protection from one or more qualities. 702.16g makes
    /// "protection from A and from B" shorthand for two separate abilities.
    record Protection(List<ProtectionQuality> qualities) implements Static {}

    /// 702.11d — hexproof variant limited to certain qualities.
    record HexproofFrom(List<ProtectionQuality> qualities) implements Static {}

    /// 702.14 — landwalk evasion, parameterized by the walked-land descriptor.
    record Landwalk(LandSelector selector) implements Static {}

    /// 702.5 — Aura's attachment restriction. {@code target} is the selector
    /// describing what this Aura may attach to (e.g., "creature", "creature
    /// you control", "nonland permanent"). Player targets (e.g., "Enchant
    /// player") are captured as a bare-type selector.
    record Enchant(Selector target) implements Static {}

    // Triggered keyword abilities ─────────────────────────────────────

    /// All parameter-less triggered keyword abilities (rule 702). Each
    /// constant corresponds to a single triggered keyword ability.
    enum TriggeredKeyword implements Triggered {
        /// 702.108 — +1/+1 on noncreature spells.
        PROWESS,
        /// 702.93 — return with +1/+1 counter.
        UNDYING,
        /// 702.79 — return with −1/−1 counter.
        PERSIST,
        /// 702.83 — +1/+1 when attacking alone.
        EXALTED,
        /// 702.100 — grow when a bigger creature enters.
        EVOLVE,
        /// 702.101 — pay {W/B} on spell cast, drain 1 from each opponent.
        EXTORT,
        /// 702.105 — +1/+1 when attacking the leader.
        DETHRONE,
        /// 702.95 — pair with an unpaired creature.
        SOULBOND,
        /// 702.115 — combat damage to players mills them.
        INGEST,
        /// 702.116 — create attacking copies per opponent.
        MYRIAD,
        /// 702.134 — +1/+1 on attacking creatures.
        MENTOR,
        /// 702.55 — trigger on creature dying.
        HAUNT,
        /// 702.85 — free exile-cast on cast.
        CASCADE,
        /// 702.40 — copy for each spell cast before.
        STORM,
        /// 702.69 — storm-like copy count via graveyard.
        GRAVESTORM,
        /// 702.121 — +1/+0 per attacker to attacking creature.
        MELEE,
        /// 702.149 — +1/+1 when attacking with bigger.
        TRAINING,
        /// 702.145 — day triggers on phase change.
        DAYBOUND,
        /// 702.145 — night triggers on phase change.
        NIGHTBOUND,
        /// 702.144 — opponent may copy triggered result.
        DEMONSTRATE,
        /// 702.159 — attraction-visit effect.
        VISIT,
        /// 702.91 — +1/+0 to other attackers.
        BATTLE_CRY,
        /// 702.92 — ETB-create-Germ-and-attach.
        LIVING_WEAPON,
        /// 702.25 — blocker gets −1/−1 (per 702.25a, triggered).
        FLANKING
    }

    /// 702.21 — "Whenever this becomes the target..., counter unless [cost]."
    record Ward(List<ManaSymbol> cost) implements Triggered {}

    // Activated keyword abilities ──────────────────────────────────────

    /// 702.6 — "Equip [type]? [cost]" activates to attach this Equipment
    /// to a target creature (rule 702.6a). The cost may be mana only
    /// ("Equip {2}") or include non-mana elements ("Equip—Discard a
    /// card.", Murderer's Axe), so the full {@link Cost} type is used.
    /// The optional {@code typeRestriction} narrows the attachable creature
    /// to a named subtype (e.g., Steelclaw Lance: "Equip Knight {1}" —
    /// attaches only to Knights).
    record Equip(@Nullable String typeRestriction, Cost cost) implements Activated {
        public Equip(Cost cost) {
            this(null, cost);
        }
    }

    /// 702.29 — "Cycling [cost]" activates to discard this card and draw.
    /// Cost is usually mana, but some variants take non-mana costs too.
    record Cycling(Cost cost) implements Activated {}
}
