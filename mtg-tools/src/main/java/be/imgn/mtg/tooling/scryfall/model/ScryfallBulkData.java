package be.imgn.mtg.tooling.scryfall.model;

import org.jspecify.annotations.Nullable;

/// Represents a bulk data download option from Scryfall.
///
/// @param id Unique ID for this bulk data item
/// @param type Type of bulk data (e.g., "oracle_cards", "rulings")
/// @param updatedAt ISO 8601 timestamp of last update
/// @param downloadUri URL to download the bulk data file
/// @param contentType MIME type of the download
/// @param contentEncoding Encoding (e.g., "gzip")
/// @param size Size of the download in bytes
public record ScryfallBulkData(
        String id,
        String type,
        String updatedAt,
        String downloadUri,
        String contentType,
        @Nullable String contentEncoding,
        long size) {}
