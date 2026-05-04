package be.imgn.mtg.engine.oracle2.parser.selector;

import static be.imgn.mtg.engine.oracle2.parser.Parsers.phrase;
import static com.google.common.labs.parse.Parser.or;

import java.util.stream.Stream;

import com.google.common.labs.parse.Parser;

import be.imgn.mtg.engine.oracle2.domain.ObjectStatus;
import be.imgn.mtg.engine.oracle2.domain.selector.StatusSelector;

/// Parser for [StatusSelector]. One arm per [ObjectStatus] value,
/// driven by `Stream.of(ObjectStatus.values())` and each value's
/// `text()` template.
///
/// Enum declaration order is `UNTAPPED` before `TAPPED` and
/// `UNFLIPPED` before `FLIPPED`, so the longer prefix wins
/// automatically.
public final class StatusSelectorParser {
    private StatusSelectorParser() {}

    public static final Parser<StatusSelector> STATUS_SELECTOR = Stream.of(ObjectStatus.values())
            .map(s -> phrase(s.text()).thenReturn(s))
            .collect(or())
            .map(StatusSelector.HasStatus::new);
}
