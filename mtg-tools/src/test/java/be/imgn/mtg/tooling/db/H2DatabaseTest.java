package be.imgn.mtg.tooling.db;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class H2DatabaseTest {

    @Test
    void createInMemoryDatabaseInitializesSchema() {
        try (var db = H2Database.createInMemory()) {
            var tableCount = db.jdbi()
                    .withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES"
                                    + " WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_TYPE = 'BASE TABLE'")
                            .mapTo(Integer.class)
                            .one());

            // Schema should have 10 base tables:
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
    void vintageViewExposesPerFaceRows() {
        try (var db = H2Database.createInMemory()) {
            db.jdbi().useHandle(handle -> {
                handle.execute("INSERT INTO format (format_name) VALUES ('vintage')");
                handle.execute("INSERT INTO card (name, oracle_text, data) VALUES ('Grizzly Bears', NULL, '{}')");
                handle.execute("INSERT INTO card (name, oracle_text, data) VALUES ('Lightning Bolt',"
                        + " 'Lightning Bolt deals 3 damage to any target.', '{}')");
                handle.execute(
                        "INSERT INTO card (name, face_1_name, face_1_oracle_text, face_2_name, face_2_oracle_text, data)"
                                + " VALUES ('Fire // Ice', 'Fire', 'Fire deals 2 damage divided as you choose.',"
                                + " 'Ice', 'Tap target permanent. Draw a card.', '{}')");
                // Mark all three cards legal in vintage.
                handle.execute("INSERT INTO legality (card_id, format_id, legality)"
                        + " SELECT c.card_id, f.format_id, 'legal'"
                        + " FROM card c CROSS JOIN format f WHERE f.format_name = 'vintage'");
            });

            // Vanilla single-face card: one row, oracle_text NULL, name not "// "-combined.
            var grizzly = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT name, oracle_text FROM vintage WHERE name = 'Grizzly Bears'")
                    .mapToMap()
                    .list());
            assertThat(grizzly).hasSize(1);
            assertThat(grizzly.get(0).get("oracle_text")).isNull();

            // Single-face card with text: one row.
            var bolt = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT oracle_text FROM vintage WHERE name = 'Lightning Bolt'")
                    .mapTo(String.class)
                    .list());
            assertThat(bolt).hasSize(1);
            assertThat(bolt.get(0)).contains("3 damage");

            // Split card: searching by face name returns the face row only,
            // with that face's oracle text — never the combined name, never the other face.
            var fire = db.jdbi().withHandle(handle -> handle.createQuery(
                            "SELECT name, oracle_text FROM vintage WHERE name = 'Fire'")
                    .mapToMap()
                    .list());
            assertThat(fire).hasSize(1);
            assertThat(fire.get(0).get("name")).isEqualTo("Fire");
            assertThat((String) fire.get(0).get("oracle_text")).contains("Fire deals");

            var ice = db.jdbi()
                    .withHandle(handle -> handle.createQuery("SELECT name, oracle_text FROM vintage WHERE name = 'Ice'")
                            .mapToMap()
                            .list());
            assertThat(ice).hasSize(1);
            assertThat((String) ice.get(0).get("oracle_text")).contains("Draw a card");

            // Combined name does NOT appear in the view.
            var combined = db.jdbi()
                    .withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM vintage WHERE name = 'Fire // Ice'")
                            .mapTo(Integer.class)
                            .one());
            assertThat(combined).isZero();
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
