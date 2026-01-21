/// Core engine module for Magic: The Gathering game implementation.
@org.jspecify.annotations.NullMarked
module be.imgn.mtg.engine {
    // Required modules
    requires be.imgn.mtg.parse;
    requires com.google.guice;
    requires com.h2database;
    requires com.squareup.moshi;
    requires okhttp3;
    requires org.jdbi.v3.core;
    requires org.jspecify;

    // Exported packages
    exports be.imgn.mtg.engine.action;
    exports be.imgn.mtg.engine.characteristics;
    exports be.imgn.mtg.engine.combat;
    exports be.imgn.mtg.engine.event;
    exports be.imgn.mtg.engine.format;
    exports be.imgn.mtg.engine.game;
    exports be.imgn.mtg.engine.object;
    exports be.imgn.mtg.engine.resolver;
    exports be.imgn.mtg.engine.rules;
    exports be.imgn.mtg.engine.state;
    exports be.imgn.mtg.engine.trigger;
    exports be.imgn.mtg.engine.turn;
    exports be.imgn.mtg.engine.result;
    exports be.imgn.mtg.engine.zone;

    // Open internal packages to Guice for reflection
    opens be.imgn.mtg.engine.action.internal to com.google.guice;
    opens be.imgn.mtg.engine.event.internal to com.google.guice;
    opens be.imgn.mtg.engine.game.internal to com.google.guice;
    opens be.imgn.mtg.engine.resolver.internal to com.google.guice;
    opens be.imgn.mtg.engine.state.internal to com.google.guice;
    opens be.imgn.mtg.engine.trigger.internal to com.google.guice;
    opens be.imgn.mtg.engine.turn.internal to com.google.guice;
    opens be.imgn.mtg.engine.zone.internal to com.google.guice;
}
