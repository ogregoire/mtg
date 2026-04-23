package be.imgn.mtg.engine.oracle.domain;

/// Predefined token definitions from Rule 111.10.
public enum PredefinedToken {
    TREASURE("Treasure"),
    FOOD("Food"),
    GOLD("Gold"),
    WALKER("Walker"),
    SHARD("Shard"),
    CLUE("Clue"),
    BLOOD("Blood"),
    POWERSTONE("Powerstone"),
    INCUBATOR("Incubator"),
    CURSED_ROLE("Cursed Role"),
    MONSTER_ROLE("Monster Role"),
    ROYAL_ROLE("Royal Role"),
    SORCERER_ROLE("Sorcerer Role"),
    VIRTUOUS_ROLE("Virtuous Role"),
    WICKED_ROLE("Wicked Role"),
    YOUNG_HERO_ROLE("Young Hero Role"),
    MAP("Map"),
    JUNK("Junk"),
    LANDER("Lander"),
    MUTAGEN("Mutagen");

    private final String text;

    PredefinedToken(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
