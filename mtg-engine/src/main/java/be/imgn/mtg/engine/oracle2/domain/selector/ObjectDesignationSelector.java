package be.imgn.mtg.engine.oracle2.domain.selector;

import static java.util.Objects.requireNonNull;

import be.imgn.mtg.engine.oracle2.domain.ObjectDesignation;

/// Selects an object by a [ObjectDesignation] — "target commander",
/// "your Ring-bearer". Designations aren't characteristics
/// (CR 109.3) — see [ObjectDesignation].
public record ObjectDesignationSelector(ObjectDesignation designation) implements ObjectPropertySelector {
    public ObjectDesignationSelector {
        requireNonNull(designation);
    }
}
