package be.imgn.mtg.engine.oracle.domain;

/// Pronouns and demonstrative phrases that oracle text uses as a subject
/// back-reference ("it", "them", "the rest", "one of them", …). Each value
/// binds at resolution time to an object introduced earlier in the same
/// clause; no value carries structure on its own. Used by [Subject.Pronoun].
public enum PronounType {
    IT,
    THEM,
    THEY,
    ITSELF,
    /// "the rest" — the complement of the already-named subset (Grapeshot:
    /// "choose one creature … the rest deals damage").
    THE_REST,
    THE_COPY,
    THE_COPIES,
    ONE_OF_THEM,
    BOTH_OF_THEM,
    EACH_OF_THEM
}
