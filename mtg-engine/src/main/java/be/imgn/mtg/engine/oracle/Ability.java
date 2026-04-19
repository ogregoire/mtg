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
/// for an ability of one of the four types. Each no-parameter keyword is a
/// singleton enum ({@code Flying.FLYING}, {@code Vigilance.VIGILANCE}, …);
/// parameterized keywords are records ({@link Ward}, {@link Equip},
/// {@link Landwalk}, {@link Protection}, …). Each directly implements its
/// underlying ability type per the rule in 702.Xa.
public sealed interface Ability {

    // ── The four ability types ────────────────────────────────────────

    sealed interface Static extends Ability {}

    sealed interface Triggered extends Ability {}

    sealed interface Activated extends Ability {}

    sealed interface Spell extends Ability {}

    // ── Written-out (non-keyword) ability forms ───────────────────────

    record StaticAbility(String text) implements Static {}

    record TriggeredAbility(
            String triggerWord, String event, @Nullable Condition interveningIf, List<Effect> effects)
            implements Triggered {}

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
    // types, per rule 702.Xa. No-parameter keywords are singleton enums
    // whose single value mirrors the class name; parametrized ones are
    // records.

    // Static evasion / combat / protection keywords ────────────────────

    /// 702.9 — evasion.
    enum Flying implements Static {
        FLYING
    }

    /// 702.19 — trample combat damage.
    enum Trample implements Static {
        TRAMPLE
    }

    /// 702.7 — first-strike combat damage step.
    enum FirstStrike implements Static {
        FIRST_STRIKE
    }

    /// 702.4 — two combat damage steps.
    enum DoubleStrike implements Static {
        DOUBLE_STRIKE
    }

    /// 702.10 — may attack / tap the turn it enters.
    enum Haste implements Static {
        HASTE
    }

    /// 702.20 — doesn't tap to attack.
    enum Vigilance implements Static {
        VIGILANCE
    }

    /// 702.17 — may block flying creatures.
    enum Reach implements Static {
        REACH
    }

    /// 702.111 — can only be blocked by two or more.
    enum Menace implements Static {
        MENACE
    }

    /// 702.13 — evasion limited to artifacts/same color.
    enum Intimidate implements Static {
        INTIMIDATE
    }

    /// 702.36 — evasion limited to artifacts/black.
    enum Fear implements Static {
        FEAR
    }

    /// 702.118 — can't be blocked by greater-power creatures.
    enum Skulk implements Static {
        SKULK
    }

    /// 702.2 — destroys anything it damages.
    enum Deathtouch implements Static {
        DEATHTOUCH
    }

    /// 702.15 — damage dealt also gains life.
    enum Lifelink implements Static {
        LIFELINK
    }

    /// 702.11 — can't be targeted by opponents.
    enum Hexproof implements Static {
        HEXPROOF
    }

    /// 702.18 — can't be targeted.
    enum Shroud implements Static {
        SHROUD
    }

    /// 702.12 — can't be destroyed.
    enum Indestructible implements Static {
        INDESTRUCTIBLE
    }

    /// 702.3 — can't attack.
    enum Defender implements Static {
        DEFENDER
    }

    /// 702.8 — may be played any time.
    enum Flash implements Static {
        FLASH
    }

    /// 702.22 — combat grouping.
    enum Banding implements Static {
        BANDING
    }

    /// 702.26 — may phase out/in.
    enum Phasing implements Static {
        PHASING
    }

    /// 702.31 — evasion over non-horsemanship creatures.
    enum Horsemanship implements Static {
        HORSEMANSHIP
    }

    /// 702.90 — damage becomes poison/−1 counters.
    enum Infect implements Static {
        INFECT
    }

    /// 702.80 — damage dealt is via −1/−1 counters.
    enum Wither implements Static {
        WITHER
    }

    /// 702.73 — every creature type.
    enum Changeling implements Static {
        CHANGELING
    }

    /// 702.114 — colorless.
    enum Devoid implements Static {
        DEVOID
    }

    /// 702.61 — can't be responded to.
    enum SplitSecond implements Static {
        SPLIT_SECOND
    }

    /// 702.66 — alternative cost using exile-from-graveyard mana.
    enum Delve implements Static {
        DELVE
    }

    /// 702.51 — alternative cost using tapping creatures.
    enum Convoke implements Static {
        CONVOKE
    }

    /// 702.127 — cast only from graveyard as the aftermath half.
    enum Aftermath implements Static {
        AFTERMATH
    }

    /// 702.131 — tracks the city's blessing.
    enum Ascend implements Static {
        ASCEND
    }

    /// 702.147 — exiled zombie tokens.
    enum Decayed implements Static {
        DECAYED
    }

    /// 702.150 — return from graveyard payment.
    enum Compleated implements Static {
        COMPLEATED
    }

    /// 702.169 — solved-case marker.
    enum Solved implements Static {
        SOLVED
    }

    /// 702.161 — vehicle that's always a creature.
    enum LivingMetal implements Static {
        LIVING_METAL
    }

    /// 702.163 — equip to a Legendary on ETB.
    enum ForMirrodin implements Static {
        FOR_MIRRODIN
    }

    /// 702.155 — lore counters add one at a time.
    enum ReadAhead implements Static {
        READ_AHEAD
    }

    /// 702.89 — indestructible aura substitute.
    enum UmbraArmor implements Static {
        UMBRA_ARMOR
    }

    /// 702.102 — split card with fused casting.
    enum Fuse implements Static {
        FUSE
    }

    /// 702.106 — face-down in the command zone.
    enum HiddenAgenda implements Static {
        HIDDEN_AGENDA
    }

    /// 702.81 — cast from graveyard by discarding.
    enum Retrace implements Static {
        RETRACE
    }

    /// 702.50 — game-ending spell.
    enum Epic implements Static {
        EPIC
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

    /// 702.5 — Aura's attachment restriction.
    record Enchant(String object) implements Static {}

    // Triggered keyword abilities ──────────────────────────────────────

    /// 702.108 — +1/+1 on noncreature spells.
    enum Prowess implements Triggered {
        PROWESS
    }

    /// 702.93 — return with +1/+1 counter.
    enum Undying implements Triggered {
        UNDYING
    }

    /// 702.79 — return with −1/−1 counter.
    enum Persist implements Triggered {
        PERSIST
    }

    /// 702.83 — +1/+1 when attacking alone.
    enum Exalted implements Triggered {
        EXALTED
    }

    /// 702.100 — grow when a bigger creature enters.
    enum Evolve implements Triggered {
        EVOLVE
    }

    /// 702.101 — pay {W/B} on spell cast, drain 1 from each opponent.
    enum Extort implements Triggered {
        EXTORT
    }

    /// 702.105 — +1/+1 when attacking the leader.
    enum Dethrone implements Triggered {
        DETHRONE
    }

    /// 702.95 — pair with an unpaired creature.
    enum Soulbond implements Triggered {
        SOULBOND
    }

    /// 702.115 — combat damage to players mills them.
    enum Ingest implements Triggered {
        INGEST
    }

    /// 702.116 — create attacking copies per opponent.
    enum Myriad implements Triggered {
        MYRIAD
    }

    /// 702.134 — +1/+1 on attacking creatures.
    enum Mentor implements Triggered {
        MENTOR
    }

    /// 702.55 — trigger on creature dying.
    enum Haunt implements Triggered {
        HAUNT
    }

    /// 702.85 — free exile-cast on cast.
    enum Cascade implements Triggered {
        CASCADE
    }

    /// 702.40 — copy for each spell cast before.
    enum Storm implements Triggered {
        STORM
    }

    /// 702.69 — storm-like copy count via graveyard.
    enum Gravestorm implements Triggered {
        GRAVESTORM
    }

    /// 702.121 — +1/+0 per attacker to attacking creature.
    enum Melee implements Triggered {
        MELEE
    }

    /// 702.149 — +1/+1 when attacking with bigger.
    enum Training implements Triggered {
        TRAINING
    }

    /// 702.145 — day triggers on phase change.
    enum Daybound implements Triggered {
        DAYBOUND
    }

    /// 702.145 — night triggers on phase change.
    enum Nightbound implements Triggered {
        NIGHTBOUND
    }

    /// 702.144 — opponent may copy triggered result.
    enum Demonstrate implements Triggered {
        DEMONSTRATE
    }

    /// 702.159 — attraction-visit effect.
    enum Visit implements Triggered {
        VISIT
    }

    /// 702.91 — +1/+0 to other attackers.
    enum BattleCry implements Triggered {
        BATTLE_CRY
    }

    /// 702.92 — ETB-create-Germ-and-attach.
    enum LivingWeapon implements Triggered {
        LIVING_WEAPON
    }

    /// 702.25 — blocker gets −1/−1 (per 702.25a, triggered).
    enum Flanking implements Triggered {
        FLANKING
    }

    /// 702.21 — "Whenever this becomes the target..., counter unless [cost]."
    record Ward(List<ManaSymbol> cost) implements Triggered {}

    // Activated keyword abilities ──────────────────────────────────────

    /// 702.6 — "Equip [cost]" activates to attach this Equipment to a
    /// target creature (rule 702.6a). The cost may be mana only ("Equip
    /// {2}") or include non-mana elements ("Equip—Discard a card.",
    /// Murderer's Axe), so the full {@link Cost} type is used.
    record Equip(Cost cost) implements Activated {}

    /// 702.29 — "Cycling [cost]" activates to discard this card and draw.
    /// Cost is usually mana, but some variants take non-mana costs too.
    record Cycling(Cost cost) implements Activated {}
}
