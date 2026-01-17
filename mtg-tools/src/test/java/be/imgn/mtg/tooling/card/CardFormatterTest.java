package be.imgn.mtg.tooling.card;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import be.imgn.mtg.tooling.card.model.CardLegalityResult;
import be.imgn.mtg.tooling.card.model.CardPrintResult;
import be.imgn.mtg.tooling.card.model.CardResult;
import be.imgn.mtg.tooling.card.model.CardRulingResult;

class CardFormatterTest {

    @Test
    void formatSingleFacedCreature() {
        var card = new CardResult(
                1L,
                "Llanowar Elves",
                "normal",
                1.0,
                "G",
                "G",
                "{G}",
                "{T}: Add {G}.",
                "1",
                "1",
                "Creature — Elf Druid",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var legalities = List.of(
                new CardLegalityResult("modern", "legal"),
                new CardLegalityResult("standard", "not_legal"),
                new CardLegalityResult("vintage", "restricted"));

        var prints = List.of(
                new CardPrintResult("Dominaria", "dom", "168"), new CardPrintResult("Core Set 2019", "m19", "314"));

        var rulings = List.of(new CardRulingResult(LocalDate.of(2004, 10, 4), "wotc", "This is a mana ability."));

        var mostRecentPrint = new CardPrintResult("Dominaria", "dom", "168");

        var result = CardFormatter.formatCard(card, legalities, prints, 2, rulings, mostRecentPrint);

        assertThat(result)
                .contains("Llanowar Elves")
                .contains("{G}")
                .contains("Creature — Elf Druid")
                .contains("{T}: Add {G}.")
                .contains("1/1")
                .contains("Color Identity: Green")
                .contains("Legal: Modern")
                .contains("Restricted: Vintage")
                .contains("Not Legal: Standard")
                .contains("Printed in 2 sets")
                .contains("Dominaria (DOM)")
                .contains("[2004-10-04]")
                .contains("This is a mana ability.")
                .contains("https://scryfall.com/card/dom/168")
                .contains("https://gatherer.wizards.com");
    }

    @Test
    void formatInstantWithNoCreatureStats() {
        var card = new CardResult(
                2L,
                "Lightning Bolt",
                "normal",
                1.0,
                "R",
                "R",
                "{R}",
                "Lightning Bolt deals 3 damage to any target.",
                null,
                null,
                "Instant",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var legalities = List.of(new CardLegalityResult("modern", "legal"));
        var prints = List.of(new CardPrintResult("Double Masters 2022", "2x2", "117"));

        var result = CardFormatter.formatCard(card, legalities, prints, 1, List.of(), prints.getFirst());

        assertThat(result)
                .contains("Lightning Bolt")
                .contains("{R}")
                .contains("Instant")
                .contains("deals 3 damage")
                .doesNotContainPattern("\\n\\d+/\\d+\\n") // No P/T line
                .contains("Color Identity: Red")
                .contains("Printed in 1 set");
    }

    @Test
    void formatPlaneswalkerWithLoyalty() {
        var card = new CardResult(
                3L,
                "Jace, the Mind Sculptor",
                "normal",
                4.0,
                "U",
                "U",
                "{2}{U}{U}",
                "+2: Look at the top card of target player's library.",
                null,
                null,
                "Legendary Planeswalker — Jace",
                "3",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var result = CardFormatter.formatCard(card, List.of(), List.of(), 0, List.of(), null);

        assertThat(result)
                .contains("Jace, the Mind Sculptor")
                .contains("{2}{U}{U}")
                .contains("Legendary Planeswalker — Jace")
                .contains("Loyalty: 3");
    }

    @Test
    void formatBattleWithDefense() {
        var card = new CardResult(
                4L,
                "Invasion of Zendikar",
                "battle",
                4.0,
                "G",
                "G",
                "{3}{G}",
                "When Invasion of Zendikar enters the battlefield, search your library for a basic land card.",
                null,
                null,
                "Battle — Siege",
                null,
                "3",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var result = CardFormatter.formatCard(card, List.of(), List.of(), 0, List.of(), null);

        assertThat(result)
                .contains("Invasion of Zendikar")
                .contains("Battle — Siege")
                .contains("Defense: 3");
    }

    @Test
    void formatDoubleFacedCard() {
        var card = new CardResult(
                5L,
                "Delver of Secrets // Insectile Aberration",
                "transform",
                1.0,
                "U",
                "U",
                "{U}",
                null,
                null,
                null,
                "Creature — Human Wizard // Creature — Human Insect",
                null,
                null,
                "Delver of Secrets",
                "{U}",
                "At the beginning of your upkeep, look at the top card of your library. You may reveal that card. If an instant or sorcery card is revealed this way, transform Delver of Secrets.",
                "Creature — Human Wizard",
                "1",
                "1",
                null,
                "Insectile Aberration",
                null,
                "Flying",
                "Creature — Human Insect",
                "3",
                "2",
                null);

        var result = CardFormatter.formatCard(card, List.of(), List.of(), 0, List.of(), null);

        // Should show side-by-side format
        assertThat(result)
                .contains("Delver of Secrets")
                .contains("Insectile Aberration")
                .contains("{U}")
                .contains("Creature — Human Wizard")
                .contains("Creature — Human Insect")
                .contains("Flying")
                .contains("1/1")
                .contains("3/2")
                .contains("|"); // Column separator
    }

    @Test
    void formatColorlessCard() {
        var card = new CardResult(
                6L,
                "Sol Ring",
                "normal",
                1.0,
                "",
                "",
                "{1}",
                "{T}: Add {C}{C}.",
                null,
                null,
                "Artifact",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var result = CardFormatter.formatCard(card, List.of(), List.of(), 0, List.of(), null);

        assertThat(result).contains("Sol Ring").contains("Color Identity: Colorless");
    }

    @Test
    void formatMultiColorCard() {
        var card = new CardResult(
                7L,
                "Teferi, Time Raveler",
                "normal",
                3.0,
                "W,U",
                "W,U",
                "{1}{W}{U}",
                "Each opponent can cast spells only any time they could cast a sorcery.",
                null,
                null,
                "Legendary Planeswalker — Teferi",
                "4",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var result = CardFormatter.formatCard(card, List.of(), List.of(), 0, List.of(), null);

        assertThat(result).contains("Color Identity: White, Blue");
    }

    @Test
    void formatSearchResultsSinglePage() {
        var cards = List.of(
                new CardResult(
                        1L,
                        "Lightning Bolt",
                        "normal",
                        1.0,
                        "R",
                        "R",
                        "{R}",
                        null,
                        null,
                        null,
                        "Instant",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                new CardResult(
                        2L,
                        "Searing Bolt",
                        "normal",
                        2.0,
                        "R",
                        "R",
                        "{1}{R}",
                        null,
                        null,
                        null,
                        "Instant",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        var result = CardFormatter.formatSearchResults(cards, 2, 1, 20, "./mtg card --name \"*Bolt*\"");

        assertThat(result)
                .contains("Found 2 cards")
                .doesNotContain("(page") // Single page, no pagination info
                .contains("Lightning Bolt")
                .contains("{R}")
                .contains("Instant")
                .contains("Searing Bolt")
                .contains("{1}{R}")
                .contains("./mtg card --name \"Lightning Bolt\"")
                .contains("./mtg card --name \"Searing Bolt\"")
                .doesNotContain("Next page:");
    }

    @Test
    void formatSearchResultsWithPagination() {
        var cards = List.of(new CardResult(
                1L,
                "Lightning Bolt",
                "normal",
                1.0,
                "R",
                "R",
                "{R}",
                null,
                null,
                null,
                "Instant",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        var result = CardFormatter.formatSearchResults(cards, 47, 1, 20, "./mtg card --name \"*Bolt*\"");

        assertThat(result)
                .contains("Found 47 cards")
                .contains("(page 1 of 3)")
                .contains("Next page: ./mtg card --name \"*Bolt*\" --page 2");
    }

    @Test
    void formatSearchResultsMiddlePage() {
        var cards = List.of(new CardResult(
                1L,
                "Test Card",
                "normal",
                1.0,
                "R",
                "R",
                "{R}",
                null,
                null,
                null,
                "Instant",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        var result = CardFormatter.formatSearchResults(cards, 60, 2, 20, "./mtg card --name \"*Bolt*\" --page 2");

        assertThat(result).contains("(page 2 of 3)").contains("Next page: ./mtg card --name \"*Bolt*\" --page 3");
    }

    @Test
    void formatSearchResultsLastPage() {
        var cards = List.of(new CardResult(
                1L,
                "Test Card",
                "normal",
                1.0,
                "R",
                "R",
                "{R}",
                null,
                null,
                null,
                "Instant",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        var result = CardFormatter.formatSearchResults(cards, 41, 3, 20, "./mtg card --name \"*Bolt*\" --page 3");

        assertThat(result).contains("(page 3 of 3)").doesNotContain("Next page:");
    }

    @Test
    void formatSearchResultsSingularCard() {
        var cards = List.of(new CardResult(
                1L,
                "Test Card",
                "normal",
                1.0,
                "R",
                "R",
                "{R}",
                null,
                null,
                null,
                "Instant",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        var result = CardFormatter.formatSearchResults(cards, 1, 1, 20, "./mtg card --name \"Test Card\"");

        assertThat(result).contains("Found 1 card").doesNotContain("cards");
    }

    @Test
    void formatCardEscapesQuotesInName() {
        var cards = List.of(new CardResult(
                1L,
                "\"Ach! Hans, Run!\"",
                "normal",
                6.0,
                "R,G",
                "R,G",
                "{2}{R}{R}{G}{G}",
                null,
                null,
                null,
                "Enchantment",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        var result = CardFormatter.formatSearchResults(cards, 1, 1, 20, "./mtg card --name \"*Ach*\"");

        assertThat(result).contains("./mtg card --name \"\\\"Ach! Hans, Run!\\\"\"");
    }

    @Test
    void formatLegalitiesGroupedByStatus() {
        var card = new CardResult(
                1L,
                "Test Card",
                "normal",
                1.0,
                "R",
                "R",
                "{R}",
                "Test text",
                null,
                null,
                "Instant",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var legalities = List.of(
                new CardLegalityResult("modern", "legal"),
                new CardLegalityResult("legacy", "legal"),
                new CardLegalityResult("standard", "not_legal"),
                new CardLegalityResult("vintage", "banned"),
                new CardLegalityResult("commander", "restricted"));

        var result = CardFormatter.formatCard(card, legalities, List.of(), 0, List.of(), null);

        assertThat(result)
                .contains("Legal: Modern, Legacy")
                .contains("Restricted: Commander")
                .contains("Banned: Vintage")
                .contains("Not Legal: Standard");
    }

    @Test
    void formatPrintsShowsTruncationMessage() {
        var card = new CardResult(
                1L,
                "Test Card",
                "normal",
                1.0,
                "R",
                "R",
                "{R}",
                "Test text",
                null,
                null,
                "Instant",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var prints = List.of(new CardPrintResult("Set 1", "s1", "1"), new CardPrintResult("Set 2", "s2", "2"));

        var result = CardFormatter.formatCard(card, List.of(), prints, 50, List.of(), prints.getFirst());

        assertThat(result).contains("Printed in 50 sets (2 shown)");
    }

    @Test
    void formatRulingsWithDates() {
        var card = new CardResult(
                1L,
                "Test Card",
                "normal",
                1.0,
                "R",
                "R",
                "{R}",
                "Test text",
                null,
                null,
                "Instant",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        var rulings = List.of(
                new CardRulingResult(LocalDate.of(2023, 6, 15), "wotc", "First ruling about the card."),
                new CardRulingResult(LocalDate.of(2023, 8, 20), "wotc", "Second ruling clarifying interaction."));

        var result = CardFormatter.formatCard(card, List.of(), List.of(), 0, rulings, null);

        assertThat(result)
                .contains("Rulings:")
                .contains("[2023-06-15] First ruling about the card.")
                .contains("[2023-08-20] Second ruling clarifying interaction.");
    }
}
