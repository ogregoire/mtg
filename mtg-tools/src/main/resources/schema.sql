-- Tables
CREATE TABLE IF NOT EXISTS card (
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    oracle_id UUID UNIQUE,
    name VARCHAR(255) NOT NULL,
    layout VARCHAR(50),
    mana_value DOUBLE,
    color_identity VARCHAR(255),
    color_indicator VARCHAR(255),
    colors VARCHAR(255),
    defense VARCHAR(10),
    hand_modifier VARCHAR(10),
    keywords TEXT,
    life_modifier VARCHAR(10),
    loyalty VARCHAR(10),
    mana_cost VARCHAR(255),
    oracle_text TEXT,
    power VARCHAR(10),
    toughness VARCHAR(10),
    type_line VARCHAR(255),
    face_1_name VARCHAR(255),
    face_1_mana_value DOUBLE,
    face_1_color_indicator VARCHAR(255),
    face_1_colors VARCHAR(255),
    face_1_defense VARCHAR(10),
    face_1_loyalty VARCHAR(10),
    face_1_mana_cost VARCHAR(255),
    face_1_oracle_text TEXT,
    face_1_power VARCHAR(10),
    face_1_toughness VARCHAR(10),
    face_1_type_line VARCHAR(255),
    face_2_name VARCHAR(255),
    face_2_mana_value DOUBLE,
    face_2_color_indicator VARCHAR(255),
    face_2_colors VARCHAR(255),
    face_2_defense VARCHAR(10),
    face_2_loyalty VARCHAR(10),
    face_2_mana_cost VARCHAR(255),
    face_2_oracle_text TEXT,
    face_2_power VARCHAR(10),
    face_2_toughness VARCHAR(10),
    face_2_type_line VARCHAR(255),
    parsed_correctly BOOLEAN DEFAULT FALSE,
    works_correctly BOOLEAN DEFAULT FALSE,
    oracle_parsed BOOLEAN DEFAULT FALSE,
    data JSON NOT NULL
);

CREATE TABLE IF NOT EXISTS card_set (
    set_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50),
    parent_set_id BIGINT,
    block VARCHAR(255),
    block_code VARCHAR(10),
    data JSON NOT NULL,
    FOREIGN KEY (parent_set_id) REFERENCES card_set(set_id)
);

CREATE TABLE IF NOT EXISTS print (
    print_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    set_id BIGINT NOT NULL,
    collector_number VARCHAR(20) NOT NULL,
    rarity VARCHAR(20) NOT NULL,
    data CLOB NOT NULL,
    CONSTRAINT constraint_print_unique UNIQUE (set_id, collector_number),
    FOREIGN KEY (card_id) REFERENCES card(card_id),
    FOREIGN KEY (set_id) REFERENCES card_set(set_id)
);

CREATE TABLE IF NOT EXISTS format (
    format_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    format_name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS legality (
    card_id BIGINT NOT NULL,
    format_id BIGINT NOT NULL,
    legality VARCHAR(20) NOT NULL,
    PRIMARY KEY (card_id, format_id),
    FOREIGN KEY (card_id) REFERENCES card(card_id),
    FOREIGN KEY (format_id) REFERENCES format(format_id)
);

CREATE TABLE IF NOT EXISTS ruling (
    ruling_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    source VARCHAR(20) NOT NULL,
    published_at DATE NOT NULL,
    comment TEXT NOT NULL,
    FOREIGN KEY (card_id) REFERENCES card(card_id)
);

-- Indexes
-- Note: we do NOT add explicit indexes for columns that already have a
-- constraint-backed index — H2 auto-creates an index for every foreign
-- key and primary key, and it "adopts" any explicit index whose column
-- list matches, which later prevents DROP INDEX during bulk-load
-- optimization. Relying on the auto-indexes avoids that trap.
--   print.card_id / print.set_id    — FK-backed
--   ruling.card_id                   — FK-backed
--   legality(card_id, format_id)     — PK-backed
CREATE INDEX IF NOT EXISTS idx_card_name ON card(name);
CREATE INDEX IF NOT EXISTS idx_card_oracle_id ON card(oracle_id);
CREATE INDEX IF NOT EXISTS idx_print_rarity ON print(rarity);
CREATE INDEX IF NOT EXISTS idx_legality_legality ON legality(legality);
CREATE INDEX IF NOT EXISTS idx_set_code ON card_set(code);
CREATE INDEX IF NOT EXISTS idx_format_name ON format(format_name);
CREATE INDEX IF NOT EXISTS idx_legality_format_legality_card ON legality(format_id, legality, card_id);

-- Rules version tracking (single row)
CREATE TABLE IF NOT EXISTS rule_version (
    version_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    effective_date DATE NOT NULL,
    downloaded_at TIMESTAMP NOT NULL,
    source_url VARCHAR(500) NOT NULL
);

-- Rules table
CREATE TABLE IF NOT EXISTS rule (
    rule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_number VARCHAR(20) NOT NULL UNIQUE,
    text TEXT NOT NULL,
    parent_rule VARCHAR(20),
    section VARCHAR(100),
    section_number VARCHAR(10)
);

-- Glossary table
CREATE TABLE IF NOT EXISTS rule_glossary (
    glossary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    term VARCHAR(100) NOT NULL UNIQUE,
    definition TEXT NOT NULL
);

-- Keyword to rule mapping
CREATE TABLE IF NOT EXISTS rule_keyword (
    keyword_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword VARCHAR(100) NOT NULL,
    rule_number VARCHAR(20) NOT NULL,
    UNIQUE (keyword, rule_number)
);

-- Rules indexes
CREATE INDEX IF NOT EXISTS idx_rule_number ON rule(rule_number);
CREATE INDEX IF NOT EXISTS idx_rule_parent ON rule(parent_rule);
CREATE INDEX IF NOT EXISTS idx_rule_section ON rule(section_number);
CREATE INDEX IF NOT EXISTS idx_glossary_term ON rule_glossary(term);
CREATE INDEX IF NOT EXISTS idx_keyword ON rule_keyword(keyword);

-- Read-only user for SQL queries
CREATE USER IF NOT EXISTS readonly PASSWORD 'readonly';
GRANT SELECT ON SCHEMA PUBLIC TO readonly;
