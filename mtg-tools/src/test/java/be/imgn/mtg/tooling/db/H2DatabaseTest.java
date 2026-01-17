package be.imgn.mtg.tooling.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class H2DatabaseTest {

    @Test
    void createInMemoryDatabaseInitializesSchema() {
        try (var db = H2Database.createInMemory()) {
            var tableCount = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")
                    .mapTo(Integer.class)
                    .one());

            // Schema should have 10 tables:
            // Cards: card, card_set, print, format, legality, ruling
            // Rules: rule_version, rule, rule_glossary, rule_keyword
            assertThat(tableCount).isEqualTo(10);
        }
    }

    @Test
    void cardTableExists() {
        try (var db = H2Database.createInMemory()) {
            var exists = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CARD' AND TABLE_SCHEMA = 'PUBLIC'")
                    .mapTo(Integer.class)
                    .one());

            assertThat(exists).isEqualTo(1);
        }
    }

    @Test
    void cardSetTableExists() {
        try (var db = H2Database.createInMemory()) {
            var exists = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CARD_SET' AND TABLE_SCHEMA = 'PUBLIC'")
                    .mapTo(Integer.class)
                    .one());

            assertThat(exists).isEqualTo(1);
        }
    }

    @Test
    void printTableExists() {
        try (var db = H2Database.createInMemory()) {
            var exists = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'PRINT' AND TABLE_SCHEMA = 'PUBLIC'")
                    .mapTo(Integer.class)
                    .one());

            assertThat(exists).isEqualTo(1);
        }
    }

    @Test
    void formatTableExists() {
        try (var db = H2Database.createInMemory()) {
            var exists = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'FORMAT' AND TABLE_SCHEMA = 'PUBLIC'")
                    .mapTo(Integer.class)
                    .one());

            assertThat(exists).isEqualTo(1);
        }
    }

    @Test
    void legalityTableExists() {
        try (var db = H2Database.createInMemory()) {
            var exists = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'LEGALITY' AND TABLE_SCHEMA = 'PUBLIC'")
                    .mapTo(Integer.class)
                    .one());

            assertThat(exists).isEqualTo(1);
        }
    }

    @Test
    void rulingTableExists() {
        try (var db = H2Database.createInMemory()) {
            var exists = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'RULING' AND TABLE_SCHEMA = 'PUBLIC'")
                    .mapTo(Integer.class)
                    .one());

            assertThat(exists).isEqualTo(1);
        }
    }

    @Test
    void jdbiInstanceIsNotNull() {
        try (var db = H2Database.createInMemory()) {
            assertThat(db.jdbi()).isNotNull();
        }
    }

    @Test
    void schemaNotReinitialized() {
        try (var db = H2Database.createInMemory()) {
            // Insert a card
            db.jdbi()
                    .useHandle(handle -> handle.createUpdate("INSERT INTO card (name, data) VALUES ('Test Card', '{}')")
                            .execute());

            var countBefore = db.jdbi().withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM card")
                    .mapTo(Integer.class)
                    .one());

            assertThat(countBefore).isEqualTo(1);

            // The schema should not be reinitialized on subsequent operations
            var exists = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'CARD' AND TABLE_SCHEMA = 'PUBLIC'")
                    .mapTo(Integer.class)
                    .one());

            assertThat(exists).isEqualTo(1);

            // Card should still be there
            var countAfter = db.jdbi().withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM card")
                    .mapTo(Integer.class)
                    .one());

            assertThat(countAfter).isEqualTo(1);
        }
    }

    @Test
    void indexesCreated() {
        try (var db = H2Database.createInMemory()) {
            var indexCount = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE TABLE_SCHEMA = 'PUBLIC' AND INDEX_NAME LIKE 'IDX_%'")
                    .mapTo(Integer.class)
                    .one());

            // We expect multiple indexes from schema.sql
            assertThat(indexCount).isGreaterThanOrEqualTo(10);
        }
    }
}
