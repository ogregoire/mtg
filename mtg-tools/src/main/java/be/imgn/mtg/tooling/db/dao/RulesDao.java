package be.imgn.mtg.tooling.db.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jspecify.annotations.Nullable;

/// Data access object for rules insert operations.
public interface RulesDao {

    // Rule version operations

    @SqlUpdate("""
            INSERT INTO rule_version (version, effective_date, downloaded_at, source_url)
            VALUES (:version, :effectiveDate, :downloadedAt, :sourceUrl)
            """)
    void insertVersion(
            @Bind("version") String version,
            @Bind("effectiveDate") LocalDate effectiveDate,
            @Bind("downloadedAt") LocalDateTime downloadedAt,
            @Bind("sourceUrl") String sourceUrl);

    @SqlUpdate("DELETE FROM rule_version")
    void deleteAllVersions();

    // Rule operations

    @SqlUpdate("""
            INSERT INTO rule (rule_number, text, parent_rule, section, section_number)
            VALUES (:ruleNumber, :text, :parentRule, :section, :sectionNumber)
            """)
    void insertRule(
            @Bind("ruleNumber") String ruleNumber,
            @Bind("text") String text,
            @Bind("parentRule") @Nullable String parentRule,
            @Bind("section") @Nullable String section,
            @Bind("sectionNumber") @Nullable String sectionNumber);

    @SqlUpdate("DELETE FROM rule")
    void deleteAllRules();

    // Glossary operations

    @SqlUpdate("""
            INSERT INTO rule_glossary (term, definition)
            VALUES (:term, :definition)
            """)
    void insertGlossary(@Bind("term") String term, @Bind("definition") String definition);

    @SqlUpdate("DELETE FROM rule_glossary")
    void deleteAllGlossary();

    // Keyword operations

    @SqlUpdate("""
            INSERT INTO rule_keyword (keyword, rule_number)
            VALUES (:keyword, :ruleNumber)
            """)
    void insertKeyword(@Bind("keyword") String keyword, @Bind("ruleNumber") String ruleNumber);

    @SqlUpdate("DELETE FROM rule_keyword")
    void deleteAllKeywords();

    // Index operations for full-text search

    @SqlUpdate("DROP INDEX IF EXISTS idx_rule_number")
    void dropRuleNumberIndex();

    @SqlUpdate("DROP INDEX IF EXISTS idx_rule_parent")
    void dropRuleParentIndex();

    @SqlUpdate("DROP INDEX IF EXISTS idx_rule_section")
    void dropRuleSectionIndex();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_rule_number ON rule(rule_number)")
    void createRuleNumberIndex();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_rule_parent ON rule(parent_rule)")
    void createRuleParentIndex();

    @SqlUpdate("CREATE INDEX IF NOT EXISTS idx_rule_section ON rule(section_number)")
    void createRuleSectionIndex();
}
