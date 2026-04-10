/// Core engine module for Magic: The Gathering game implementation.
@org.jspecify.annotations.NullMarked
module be.imgn.mtg.engine {
    // Required modules
    requires com.google.common.labs.regex;
    requires com.google.mu;
    requires com.google.guice;
    requires com.h2database;
    requires com.squareup.moshi;
    requires okhttp3;
    requires org.jdbi.v3.core;
    requires java.sql;
    requires org.antlr.antlr4.runtime;
    requires org.jspecify;
    requires jdk.compiler;

    // Exported packages
    exports be.imgn.mtg.engine.card;
    exports be.imgn.mtg.engine.ability;
    exports be.imgn.mtg.engine.action;
    exports be.imgn.mtg.engine.characteristics;
    exports be.imgn.mtg.engine.cost;
    exports be.imgn.mtg.engine.combat;
    exports be.imgn.mtg.engine.effect;
    exports be.imgn.mtg.engine.event;
    exports be.imgn.mtg.engine.format;
    exports be.imgn.mtg.engine.game;
    exports be.imgn.mtg.engine.mana;
    exports be.imgn.mtg.engine.object;
    exports be.imgn.mtg.engine.resolver;
    exports be.imgn.mtg.engine.rules;
    exports be.imgn.mtg.engine.state;
    exports be.imgn.mtg.engine.trigger;
    exports be.imgn.mtg.engine.turn;
    exports be.imgn.mtg.engine.result;
    exports be.imgn.mtg.engine.selector;
    exports be.imgn.mtg.engine.zone;

    // Open internal packages to Guice for reflection
    opens be.imgn.mtg.engine.card.internal to com.google.guice;
    opens be.imgn.mtg.engine.ability.internal to com.google.guice;
    opens be.imgn.mtg.engine.cost.internal to com.google.guice;
    opens be.imgn.mtg.engine.action.internal to com.google.guice;
    opens be.imgn.mtg.engine.event.internal to com.google.guice;
    opens be.imgn.mtg.engine.game.internal to com.google.guice;
    opens be.imgn.mtg.engine.mana.internal to com.google.guice;
    opens be.imgn.mtg.engine.resolver.internal to com.google.guice;
    opens be.imgn.mtg.engine.state.internal to com.google.guice;
    opens be.imgn.mtg.engine.trigger.internal to com.google.guice;
    opens be.imgn.mtg.engine.turn.internal to com.google.guice;
    opens be.imgn.mtg.engine.turn.internal.sba to com.google.guice;
    opens be.imgn.mtg.engine.zone.internal to com.google.guice;
}
