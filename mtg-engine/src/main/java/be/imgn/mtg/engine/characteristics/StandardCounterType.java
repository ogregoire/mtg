package be.imgn.mtg.engine.characteristics;

/// Standard counter types defined in the Magic rules ({@mtg.rule 122}).
///
/// These counter types have special rules associated with them. Counter types not listed
/// here can be represented using {@link CustomCounterType}.
public enum StandardCounterType implements CounterType {

    // === Power/Toughness Counters (Rule 122.1a) ===

    /// +1/+1 counters, which increase power and toughness.
    PLUS_ONE_PLUS_ONE("+1/+1"),
    /// -1/-1 counters, which decrease power and toughness.
    MINUS_ONE_MINUS_ONE("-1/-1"),

    // === Keyword Counters (Rule 122.1b) ===

    /// Flying keyword counter.
    FLYING("flying"),
    /// First strike keyword counter.
    FIRST_STRIKE("first strike"),
    /// Double strike keyword counter.
    DOUBLE_STRIKE("double strike"),
    /// Deathtouch keyword counter.
    DEATHTOUCH("deathtouch"),
    /// Decayed keyword counter.
    DECAYED("decayed"),
    /// Exalted keyword counter.
    EXALTED("exalted"),
    /// Haste keyword counter.
    HASTE("haste"),
    /// Hexproof keyword counter.
    HEXPROOF("hexproof"),
    /// Indestructible keyword counter.
    INDESTRUCTIBLE("indestructible"),
    /// Lifelink keyword counter.
    LIFELINK("lifelink"),
    /// Menace keyword counter.
    MENACE("menace"),
    /// Reach keyword counter.
    REACH("reach"),
    /// Shadow keyword counter.
    SHADOW("shadow"),
    /// Trample keyword counter.
    TRAMPLE("trample"),
    /// Vigilance keyword counter.
    VIGILANCE("vigilance"),

    // === Special Rule Counters (Rules 122.1c-122.1i) ===

    /// Shield counters protect permanents from destruction and damage ({@mtg.rule 122.1c}).
    SHIELD("shield"),
    /// Stun counters prevent permanents from untapping ({@mtg.rule 122.1d}).
    STUN("stun"),
    /// Loyalty counters, used on planeswalkers ({@mtg.rule 122.1e}).
    LOYALTY("loyalty"),
    /// Poison counters on players; 10+ causes loss ({@mtg.rule 122.1f}).
    POISON("poison"),
    /// Defense counters, used on battles ({@mtg.rule 122.1g}).
    DEFENSE("defense"),
    /// Finality counters prevent going to graveyard ({@mtg.rule 122.1h}).
    FINALITY("finality"),
    /// Rad counters trigger at precombat main phase ({@mtg.rule 122.1i}).
    RAD("rad"),

    // === Other Rules-Referenced Counters ===

    /// Energy counters on players ({@mtg.rule 107.14}).
    ENERGY("energy"),
    /// Lore counters, used on Sagas ({@mtg.rule 714}).
    LORE("lore"),
    /// Ticket counters ({@mtg.rule 107.17}).
    TICKET("ticket"),
    /// Level counters, used on level up cards ({@mtg.rule 711}).
    LEVEL("level"),
    /// Time counters, used with suspend and vanishing ({@mtg.rule 702.62}, {@mtg.rule 702.63}).
    TIME("time"),
    /// Age counters, used with cumulative upkeep ({@mtg.rule 702.24}).
    AGE("age"),
    /// Charge counters, used with sunburst and other mechanics ({@mtg.rule 702.44}).
    CHARGE("charge");

    private final String text;

    StandardCounterType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
