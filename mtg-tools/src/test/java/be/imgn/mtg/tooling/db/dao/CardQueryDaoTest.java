package be.imgn.mtg.tooling.db.dao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.imgn.mtg.tooling.db.H2Database;

class CardQueryDaoTest {

    private H2Database db;
    private CardQueryDao dao;

    @BeforeEach
    void setUp() {
        db = H2Database.createInMemory();
        dao = new CardQueryDao(db.jdbi());
        insertTestData();
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    private void insertTestData() {
        db.jdbi().useHandle(handle -> {
            // Insert cards
            handle.createUpdate("""
                    INSERT INTO card (card_id, name, layout, mana_value, color_identity, colors, mana_cost,
                        oracle_text, power, toughness, type_line, data)
                    VALUES (1, 'Lightning Bolt', 'normal', 1.0, 'R', 'R', '{R}',
                        'Lightning Bolt deals 3 damage to any target.', NULL, NULL, 'Instant', '{}')
                    """).execute();

            handle.createUpdate("""
                    INSERT INTO card (card_id, name, layout, mana_value, color_identity, colors, mana_cost,
                        oracle_text, power, toughness, type_line, data)
                    VALUES (2, 'Llanowar Elves', 'normal', 1.0, 'G', 'G', '{G}',
                        '{T}: Add {G}.', '1', '1', 'Creature — Elf Druid', '{}')
                    """).execute();

            handle.createUpdate("""
                    INSERT INTO card (card_id, name, layout, mana_value, color_identity, colors, mana_cost,
                        oracle_text, power, toughness, type_line, data)
                    VALUES (3, 'Searing Bolt', 'normal', 2.0, 'R', 'R', '{1}{R}',
                        'Searing Bolt deals 2 damage to any target.', NULL, NULL, 'Instant', '{}')
                    """).execute();

            // Double-faced card
            handle.createUpdate("""
                    INSERT INTO card (card_id, name, layout, mana_value, color_identity, colors, mana_cost,
                        oracle_text, type_line,
                        face_1_name, face_1_mana_cost, face_1_oracle_text, face_1_type_line, face_1_power, face_1_toughness,
                        face_2_name, face_2_oracle_text, face_2_type_line, face_2_power, face_2_toughness, data)
                    VALUES (4, 'Delver of Secrets // Insectile Aberration', 'transform', 1.0, 'U', 'U', '{U}',
                        NULL, 'Creature — Human Wizard // Creature — Human Insect',
                        'Delver of Secrets', '{U}', 'At the beginning of your upkeep, look at the top card.', 'Creature — Human Wizard', '1', '1',
                        'Insectile Aberration', 'Flying', 'Creature — Human Insect', '3', '2', '{}')
                    """).execute();

            // Colorless card
            handle.createUpdate("""
                    INSERT INTO card (card_id, name, layout, mana_value, color_identity, colors, mana_cost,
                        oracle_text, type_line, data)
                    VALUES (5, 'Sol Ring', 'normal', 1.0, '', '', '{1}',
                        '{T}: Add {C}{C}.', 'Artifact', '{}')
                    """).execute();

            // Multicolor card
            handle.createUpdate("""
                    INSERT INTO card (card_id, name, layout, mana_value, color_identity, colors, mana_cost,
                        oracle_text, power, toughness, type_line, data)
                    VALUES (6, 'Teferi, Hero of Dominaria', 'normal', 5.0, 'W,U', 'W,U', '{3}{W}{U}',
                        '+1: Draw a card.', NULL, NULL, 'Legendary Planeswalker — Teferi', '{}')
                    """).execute();

            // Insert sets
            handle.createUpdate(
                            "INSERT INTO card_set (set_id, code, name, type, data) VALUES (1, '2x2', 'Double Masters 2022', 'masters', '{}')")
                    .execute();
            handle.createUpdate(
                            "INSERT INTO card_set (set_id, code, name, type, data) VALUES (2, 'dom', 'Dominaria', 'expansion', '{}')")
                    .execute();
            handle.createUpdate(
                            "INSERT INTO card_set (set_id, code, name, type, data) VALUES (3, 'm19', 'Core Set 2019', 'core', '{}')")
                    .execute();

            // Insert prints
            handle.createUpdate(
                            "INSERT INTO print (card_id, set_id, collector_number, rarity, data) VALUES (1, 1, '117', 'common', '{}')")
                    .execute();
            handle.createUpdate(
                            "INSERT INTO print (card_id, set_id, collector_number, rarity, data) VALUES (2, 2, '168', 'common', '{}')")
                    .execute();
            handle.createUpdate(
                            "INSERT INTO print (card_id, set_id, collector_number, rarity, data) VALUES (2, 3, '314', 'common', '{}')")
                    .execute();

            // Insert formats
            handle.createUpdate("INSERT INTO format (format_id, format_name) VALUES (1, 'modern')")
                    .execute();
            handle.createUpdate("INSERT INTO format (format_id, format_name) VALUES (2, 'standard')")
                    .execute();
            handle.createUpdate("INSERT INTO format (format_id, format_name) VALUES (3, 'vintage')")
                    .execute();

            // Insert legalities
            handle.createUpdate("INSERT INTO legality (card_id, format_id, legality) VALUES (1, 1, 'legal')")
                    .execute();
            handle.createUpdate("INSERT INTO legality (card_id, format_id, legality) VALUES (1, 2, 'not_legal')")
                    .execute();
            handle.createUpdate("INSERT INTO legality (card_id, format_id, legality) VALUES (1, 3, 'restricted')")
                    .execute();
            handle.createUpdate("INSERT INTO legality (card_id, format_id, legality) VALUES (2, 1, 'legal')")
                    .execute();
            handle.createUpdate("INSERT INTO legality (card_id, format_id, legality) VALUES (2, 2, 'legal')")
                    .execute();

            // Insert rulings
            handle.createUpdate(
                            "INSERT INTO ruling (card_id, source, published_at, comment) VALUES (1, 'wotc', '2004-10-04', 'Lightning Bolt deals 3 damage.')")
                    .execute();
            handle.createUpdate(
                            "INSERT INTO ruling (card_id, source, published_at, comment) VALUES (1, 'wotc', '2009-10-01', 'This is a red instant.')")
                    .execute();
        });
    }

    @Test
    void findByExactNameFindsCard() {
        var result = dao.findByExactName("Lightning Bolt");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Lightning Bolt");
        assertThat(result.get().manaCost()).isEqualTo("{R}");
    }

    @Test
    void findByExactNameIsCaseInsensitive() {
        var result = dao.findByExactName("lightning bolt");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Lightning Bolt");
    }

    @Test
    void findByExactNameReturnsEmptyForNoMatch() {
        var result = dao.findByExactName("Nonexistent Card");

        assertThat(result).isEmpty();
    }

    @Test
    void findByExactNameMatchesFace1Name() {
        var result = dao.findByExactName("Delver of Secrets");

        assertThat(result).isPresent();
        assertThat(result.get().face1Name()).isEqualTo("Delver of Secrets");
    }

    @Test
    void findByExactNameMatchesFace2Name() {
        var result = dao.findByExactName("Insectile Aberration");

        assertThat(result).isPresent();
        assertThat(result.get().face2Name()).isEqualTo("Insectile Aberration");
    }

    @Test
    void searchCardsByNamePattern() {
        var results = dao.searchCards("%Bolt%", null, null, null, null, null, 20, 0);

        assertThat(results).hasSize(2);
        assertThat(results).extracting("name").containsExactly("Lightning Bolt", "Searing Bolt");
    }

    @Test
    void searchCardsWithTypeCondition() {
        var results = dao.searchCards("%", "LOWER(type_line) LIKE LOWER('%Creature%')", null, null, null, null, 20, 0);

        assertThat(results).hasSize(2);
        assertThat(results).extracting("name").contains("Llanowar Elves", "Delver of Secrets // Insectile Aberration");
    }

    @Test
    void searchCardsWithOraclePattern() {
        var results = dao.searchCards("%", null, "%damage%", null, null, null, 20, 0);

        assertThat(results).hasSize(2);
        assertThat(results).extracting("name").containsExactly("Lightning Bolt", "Searing Bolt");
    }

    @Test
    void searchCardsWithColorCondition() {
        var results = dao.searchCards("%", null, null, "colors LIKE '%G%'", null, null, 20, 0);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name()).isEqualTo("Llanowar Elves");
    }

    @Test
    void searchCardsWithColorIdentityCondition() {
        var results = dao.searchCards("%", null, null, null, "color_identity LIKE '%W%'", null, 20, 0);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name()).isEqualTo("Teferi, Hero of Dominaria");
    }

    @Test
    void searchCardsWithDfcCondition() {
        var results = dao.searchCards(
                "%", null, null, null, null, "face_1_name IS NOT NULL AND face_2_name IS NOT NULL", 20, 0);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name()).isEqualTo("Delver of Secrets // Insectile Aberration");
    }

    @Test
    void searchCardsWithPagination() {
        var page1 = dao.searchCards("%", null, null, null, null, null, 2, 0);
        var page2 = dao.searchCards("%", null, null, null, null, null, 2, 2);

        assertThat(page1).hasSize(2);
        assertThat(page2).hasSize(2);
        assertThat(page1)
                .extracting("name")
                .doesNotContainAnyElementsOf(page2.stream().map(c -> c.name()).toList());
    }

    @Test
    void countCardsReturnsCorrectCount() {
        var count = dao.countCards("%Bolt%", null, null, null, null, null);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countCardsWithFilters() {
        var count = dao.countCards("%", "LOWER(type_line) LIKE LOWER('%Creature%')", null, null, null, null);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void searchByFormatFindsLegalCards() {
        var results = dao.searchByFormat("%", "modern", 20, 0);

        assertThat(results).hasSize(2);
        assertThat(results).extracting("name").containsExactly("Lightning Bolt", "Llanowar Elves");
    }

    @Test
    void searchByFormatIsCaseInsensitive() {
        var results = dao.searchByFormat("%", "MODERN", 20, 0);

        assertThat(results).hasSize(2);
    }

    @Test
    void countByFormatReturnsCorrectCount() {
        var count = dao.countByFormat("%", "modern");

        assertThat(count).isEqualTo(2);
    }

    @Test
    void searchBySetFindsCards() {
        var results = dao.searchBySet("%", "Dominaria", 20, 0);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name()).isEqualTo("Llanowar Elves");
    }

    @Test
    void searchBySetWithWildcard() {
        var results = dao.searchBySet("%", "%Core%", 20, 0);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().name()).isEqualTo("Llanowar Elves");
    }

    @Test
    void countBySetReturnsCorrectCount() {
        var count = dao.countBySet("%", "Dominaria");

        assertThat(count).isEqualTo(1);
    }

    @Test
    void getLegalitiesReturnsAllFormats() {
        var legalities = dao.getLegalities(1);

        assertThat(legalities).hasSize(3);
        assertThat(legalities).extracting("formatName").containsExactly("modern", "standard", "vintage");
        assertThat(legalities).extracting("legality").containsExactly("legal", "not_legal", "restricted");
    }

    @Test
    void getLegalitiesReturnsEmptyForNoLegalities() {
        var legalities = dao.getLegalities(5);

        assertThat(legalities).isEmpty();
    }

    @Test
    void countSetsReturnsCorrectCount() {
        var count = dao.countSets(2);

        assertThat(count).isEqualTo(2);
    }

    @Test
    void countSetsReturnsZeroForNoPrints() {
        var count = dao.countSets(3);

        assertThat(count).isEqualTo(0);
    }

    @Test
    void getPrintsReturnsMostRecentFirst() {
        var prints = dao.getPrints(2);

        assertThat(prints).hasSize(2);
        // Most recent print first (higher print_id)
        assertThat(prints.getFirst().setCode()).isEqualTo("m19");
        assertThat(prints.get(1).setCode()).isEqualTo("dom");
    }

    @Test
    void getPrintsReturnsEmptyForNoPrints() {
        var prints = dao.getPrints(3);

        assertThat(prints).isEmpty();
    }

    @Test
    void getMostRecentPrintReturnsLatest() {
        var print = dao.getMostRecentPrint(2);

        assertThat(print).isPresent();
        assertThat(print.get().setCode()).isEqualTo("m19");
        assertThat(print.get().collectorNumber()).isEqualTo("314");
    }

    @Test
    void getMostRecentPrintReturnsEmptyForNoPrints() {
        var print = dao.getMostRecentPrint(3);

        assertThat(print).isEmpty();
    }

    @Test
    void getRulingsReturnsAllRulings() {
        var rulings = dao.getRulings(1);

        assertThat(rulings).hasSize(2);
        // Most recent first
        assertThat(rulings.getFirst().publishedAt()).isEqualTo(LocalDate.of(2009, 10, 1));
        assertThat(rulings.get(1).publishedAt()).isEqualTo(LocalDate.of(2004, 10, 4));
    }

    @Test
    void getRulingsReturnsEmptyForNoRulings() {
        var rulings = dao.getRulings(2);

        assertThat(rulings).isEmpty();
    }

    @Test
    void isDoubleFacedReturnsTrueForDfc() {
        var result = dao.findByExactName("Delver of Secrets");

        assertThat(result).isPresent();
        assertThat(result.get().isDoubleFaced()).isTrue();
    }

    @Test
    void isDoubleFacedReturnsFalseForNormalCard() {
        var result = dao.findByExactName("Lightning Bolt");

        assertThat(result).isPresent();
        assertThat(result.get().isDoubleFaced()).isFalse();
    }
}
