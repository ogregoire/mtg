package be.imgn.mtg.engine.oracle2.domain;

/// Game-object class ({@mtg.rule 109.1}) — what kind of object oracle
/// text is referring to: a permanent, a token, a spell, an ability,
/// a copy, a card, or an emblem. Distinct from card-type
/// characteristics (creature, artifact, …) which live as
/// [be.imgn.mtg.engine.oracle2.domain.selector.CardTypeSelector] arms.
///
/// Mirrors the seven leaf records under
/// [be.imgn.mtg.engine.oracle2.domain.selector.ObjectTypeSelector].
public enum ObjectType implements Parseable {
    PERMANENT("Permanent(s)"),
    TOKEN("Token(s)"),
    SPELL("Spell(s)"),
    ABILITY("[Ability|Abilities]"),
    COPY("[Copy|Copies]"),
    CARD("Card(s)"),
    EMBLEM("Emblem(s)");

    private final String text;

    ObjectType(String text) {
        this.text = text;
    }

    @Override
    public String text() {
        return text;
    }
}
