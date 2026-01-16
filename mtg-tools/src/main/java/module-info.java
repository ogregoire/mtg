/// MTG Tools module.
///
/// Provides utilities and tooling for the MTG engine.
module be.imgn.mtg.tools {
    requires jdk.javadoc;

    exports be.imgn.mtg.tools.javadoc;

    provides jdk.javadoc.doclet.Taglet with be.imgn.mtg.tools.javadoc.MtgRuleTaglet;
}
