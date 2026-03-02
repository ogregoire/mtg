package be.imgn.mtg.tooling.db.dao;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jdbi.v3.core.Jdbi;

import be.imgn.mtg.tooling.rules.model.GlossaryResult;
import be.imgn.mtg.tooling.rules.model.RuleResult;
import be.imgn.mtg.tooling.rules.model.RuleVersionResult;
import be.imgn.mtg.tooling.rules.model.SearchResult;

/// Data access object for rules queries (read operations).
public final class RulesQueryDao {

    private final Jdbi jdbi;

    public RulesQueryDao(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    /// Gets the current rules version.
    ///
    /// @return the version info if available
    public Optional<RuleVersionResult> getVersion() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT version, effective_date, downloaded_at, source_url
                        FROM rule_version
                        ORDER BY version_id DESC
                        LIMIT 1
                        """)
                .map((rs, ctx) -> new RuleVersionResult(
                        rs.getString("version"),
                        rs.getDate("effective_date").toLocalDate(),
                        rs.getTimestamp("downloaded_at").toLocalDateTime(),
                        rs.getString("source_url")))
                .findOne());
    }

    /// Checks if rules have been synced.
    ///
    /// @return true if rules exist in the database
    public boolean hasRules() {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM rule")
                        .mapTo(Integer.class)
                        .one()
                > 0);
    }

    /// Finds a rule by exact rule number.
    ///
    /// @param ruleNumber the rule number (e.g., "702.9")
    /// @return the rule if found
    public Optional<RuleResult> findByRuleNumber(String ruleNumber) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT rule_number, text, parent_rule, section, section_number
                        FROM rule
                        WHERE rule_number = :ruleNumber
                        """)
                .bind("ruleNumber", ruleNumber)
                .map((rs, ctx) -> new RuleResult(
                        rs.getString("rule_number"),
                        rs.getString("text"),
                        rs.getString("parent_rule"),
                        rs.getString("section"),
                        rs.getString("section_number")))
                .findOne());
    }

    /// Finds a rule and all its sub-rules.
    ///
    /// @param ruleNumber the parent rule number (e.g., "702.9")
    /// @return list of rules including the parent and sub-rules
    public List<RuleResult> findRuleWithSubRules(String ruleNumber) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT rule_number, text, parent_rule, section, section_number
                        FROM rule
                        WHERE rule_number = :ruleNumber
                           OR parent_rule = :ruleNumber
                           OR rule_number LIKE :ruleNumberPrefix
                        ORDER BY rule_number
                        """)
                .bind("ruleNumber", ruleNumber)
                .bind("ruleNumberPrefix", ruleNumber + ".%")
                .map((rs, ctx) -> new RuleResult(
                        rs.getString("rule_number"),
                        rs.getString("text"),
                        rs.getString("parent_rule"),
                        rs.getString("section"),
                        rs.getString("section_number")))
                .list());
    }

    /// Finds all rules in a section.
    ///
    /// @param sectionNumber the section number (e.g., "702")
    /// @return list of rules in the section
    public List<RuleResult> findBySection(String sectionNumber) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT rule_number, text, parent_rule, section, section_number
                        FROM rule
                        WHERE section_number = :sectionNumber
                        ORDER BY rule_number
                        """)
                .bind("sectionNumber", sectionNumber)
                .map((rs, ctx) -> new RuleResult(
                        rs.getString("rule_number"),
                        rs.getString("text"),
                        rs.getString("parent_rule"),
                        rs.getString("section"),
                        rs.getString("section_number")))
                .list());
    }

    /// Finds the rule number for a keyword ability.
    ///
    /// @param keyword the keyword (case-insensitive)
    /// @return the main rule number for the keyword
    public Optional<String> findKeywordRuleNumber(String keyword) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT rule_number
                        FROM rule_keyword
                        WHERE LOWER(keyword) = LOWER(:keyword)
                        LIMIT 1
                        """)
                .bind("keyword", keyword)
                .mapTo(String.class)
                .findOne());
    }

    /// Searches rules using text matching.
    ///
    /// @param query the search query
    /// @param limit maximum number of results
    /// @return list of search results ordered by relevance
    public List<SearchResult> searchRules(String query, int limit) {
        var pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT rule_number, text, section,
                               (LENGTH(text) - LENGTH(REPLACE(LOWER(text), LOWER(:query), ''))) / LENGTH(:query) AS score
                        FROM rule
                        WHERE LOWER(text) LIKE :pattern
                        ORDER BY score DESC, rule_number
                        LIMIT :limit
                        """)
                .bind("query", query)
                .bind("pattern", pattern)
                .bind("limit", limit)
                .map((rs, ctx) -> new SearchResult(
                        rs.getString("rule_number"),
                        rs.getString("text"),
                        rs.getString("section"),
                        rs.getDouble("score")))
                .list());
    }

    /// Finds a glossary entry by exact term (case-insensitive).
    ///
    /// @param term the glossary term
    /// @return the glossary entry if found
    public Optional<GlossaryResult> findGlossaryByTerm(String term) {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT term, definition
                        FROM rule_glossary
                        WHERE LOWER(term) = LOWER(:term)
                        """)
                .bind("term", term)
                .map((rs, ctx) -> new GlossaryResult(rs.getString("term"), rs.getString("definition")))
                .findOne());
    }

    /// Searches glossary entries.
    ///
    /// @param query the search query
    /// @param limit maximum number of results
    /// @return list of matching glossary entries
    public List<GlossaryResult> searchGlossary(String query, int limit) {
        var pattern = "%" + query.toLowerCase(Locale.ROOT) + "%";
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT term, definition
                        FROM rule_glossary
                        WHERE LOWER(term) LIKE :pattern
                           OR LOWER(definition) LIKE :pattern
                        ORDER BY
                            CASE WHEN LOWER(term) = LOWER(:query) THEN 0
                                 WHEN LOWER(term) LIKE :pattern THEN 1
                                 ELSE 2
                            END,
                            term
                        LIMIT :limit
                        """)
                .bind("query", query)
                .bind("pattern", pattern)
                .bind("limit", limit)
                .map((rs, ctx) -> new GlossaryResult(rs.getString("term"), rs.getString("definition")))
                .list());
    }

    /// Finds rules that reference a specific rule number.
    ///
    /// @param ruleNumber the rule number to find references to
    /// @param limit maximum number of results
    /// @return list of rules that reference the given rule
    public List<RuleResult> findCrossReferences(String ruleNumber, int limit) {
        var pattern = "%" + ruleNumber + "%";
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT rule_number, text, parent_rule, section, section_number
                        FROM rule
                        WHERE text LIKE :pattern
                          AND rule_number != :ruleNumber
                          AND rule_number NOT LIKE :ruleNumberPrefix
                        ORDER BY rule_number
                        LIMIT :limit
                        """)
                .bind("ruleNumber", ruleNumber)
                .bind("ruleNumberPrefix", ruleNumber + "%")
                .bind("pattern", pattern)
                .bind("limit", limit)
                .map((rs, ctx) -> new RuleResult(
                        rs.getString("rule_number"),
                        rs.getString("text"),
                        rs.getString("parent_rule"),
                        rs.getString("section"),
                        rs.getString("section_number")))
                .list());
    }

    /// Gets all keywords and their rule numbers.
    ///
    /// @return list of keyword to rule number mappings
    public List<KeywordMapping> getAllKeywords() {
        return jdbi.withHandle(handle -> handle.createQuery("""
                        SELECT keyword, rule_number
                        FROM rule_keyword
                        ORDER BY keyword
                        """)
                .map((rs, ctx) -> new KeywordMapping(rs.getString("keyword"), rs.getString("rule_number")))
                .list());
    }

    /// Keyword to rule number mapping.
    public record KeywordMapping(String keyword, String ruleNumber) {}
}
