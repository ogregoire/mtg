package be.imgn.mtg.engine.effect;

import be.imgn.mtg.engine.ability.internal.parser.reference.Subject;
import be.imgn.mtg.engine.ability.internal.parser.selector.LibraryPosition;

/// An effect that puts one or more objects on top or bottom of their owner's library.
///
/// @param subject what to put on the library
/// @param position where to put it (top or bottom)
public record PutOnLibraryEffect(Subject subject, LibraryPosition position) implements Effect {}
