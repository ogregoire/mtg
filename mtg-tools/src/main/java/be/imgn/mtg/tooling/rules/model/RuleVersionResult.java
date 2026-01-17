package be.imgn.mtg.tooling.rules.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/// Represents the version information for the Comprehensive Rules.
///
/// @param version the rules version string
/// @param effectiveDate the date the rules became effective
/// @param downloadedAt when the rules were downloaded
/// @param sourceUrl the URL from which the rules were downloaded
public record RuleVersionResult(
        String version, LocalDate effectiveDate, LocalDateTime downloadedAt, String sourceUrl) {}
