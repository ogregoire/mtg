/// Core engine module for Magic: The Gathering game implementation.
@org.jspecify.annotations.NullMarked
module be.imgn.mtg.engine {
    // Required modules
    requires com.google.guice;
    requires okhttp3;
    requires com.squareup.moshi;
    requires com.h2database;
    requires org.jdbi.v3.core;
    requires org.jspecify;
    requires be.imgn.mtg.parse;

    // Exported packages
    exports be.imgn.mtg.engine.event;
    exports be.imgn.mtg.engine.characteristics;
    exports be.imgn.mtg.engine.object;

    // Open internal packages to Guice for reflection
    opens be.imgn.mtg.engine.event.internal to com.google.guice;
    opens be.imgn.mtg.engine.game.internal to com.google.guice;

    exports be.imgn.mtg.engine.turn;
    exports be.imgn.mtg.engine.game;
    exports be.imgn.mtg.engine.format;
    exports be.imgn.mtg.engine.rules;

    // Ability parsing packages
    exports be.imgn.mtg.engine.ability.internal.parser.effect;
    exports be.imgn.mtg.engine.ability.internal.parser.selector;
    exports be.imgn.mtg.engine.ability.internal.parser.reference;
    exports be.imgn.mtg.engine.ability.internal.parser;
}
