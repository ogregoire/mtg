package be.imgn.mtg.engine.characteristics.internal;

import be.imgn.mtg.engine.characteristics.Subtypes;
import be.imgn.mtg.engine.characteristics.Supertypes;
import be.imgn.mtg.engine.characteristics.Types;

/// Result of parsing a type line.
///
/// @param supertypes the parsed supertypes (may be empty)
/// @param types the parsed types
/// @param subtypes the parsed subtypes (may be empty)
public record ParsedTypeLine(Supertypes supertypes, Types types, Subtypes subtypes) {}
