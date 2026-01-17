/// MTG Tools module.
///
/// Provides utilities and tooling for the MTG engine.
module be.imgn.mtg.tools {
    requires jdk.javadoc;
    requires java.sql;
    requires com.h2database;
    requires okhttp3;
    requires com.squareup.moshi;
    requires org.jdbi.v3.core;
    requires org.jdbi.v3.sqlobject;
    requires org.jspecify;
    requires org.slf4j.nop;

    exports be.imgn.mtg.tools.javadoc;
    exports be.imgn.mtg.tooling;
    exports be.imgn.mtg.tooling.card;
    exports be.imgn.mtg.tooling.card.model;
    exports be.imgn.mtg.tooling.rules;
    exports be.imgn.mtg.tooling.rules.model;

    provides jdk.javadoc.doclet.Taglet with be.imgn.mtg.tools.javadoc.MtgRuleTaglet;
}
