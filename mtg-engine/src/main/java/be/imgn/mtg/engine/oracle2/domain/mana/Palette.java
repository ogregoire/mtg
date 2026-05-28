package be.imgn.mtg.engine.oracle2.domain.mana;

import static java.util.Objects.requireNonNull;

import java.util.List;

import be.imgn.mtg.engine.oracle2.domain.selector.ObjectSelector;

/// The set of mana symbols a "choose one of" mana shape draws from.
/// Used by [ProducedMana.OfOneColor], [ProducedMana.OfDistinctColors],
/// and [ProducedMana.Mixed].
///
/// Four arms — one static and three dynamic (resolved at the time
/// the ability resolves, per {@mtg.rule 106.7}):
///
/// - [Explicit] — fixed list of symbols (typically the five basic
///   colours, or Orcish Lumberjack's "{R} and/or {G}" subset).
/// - [CouldProduce] — colours / types the named source's mana
///   abilities *could* produce (Reflecting Pool, Star Compass,
///   Squandered Resources).
/// - [Produced] — colours / types the named source actually
///   *produced* the last time it was tapped for mana (Mirari's Wake,
///   Sisay, Kinnan, Heartbeat of Spring's "that land produced").
/// - [AmongColorsOf] — colours appearing on a set of objects (Mox
///   Amber: "any colour among legendary creatures and planeswalkers
///   you control").
public sealed interface Palette
        permits Palette.Explicit, Palette.CouldProduce, Palette.Produced, Palette.AmongColorsOf {

    /// Static palette — an explicit list of [ManaSymbol]s. Typically
    /// the five basic colours, or a restricted subset like Orcish
    /// Lumberjack's "{R} and/or {G}".
    record Explicit(List<ManaSymbol> symbols) implements Palette {
        public Explicit {
            symbols = List.copyOf(symbols);
            if (symbols.isEmpty()) {
                throw new IllegalArgumentException("Palette.Explicit must contain at least one symbol");
            }
        }
    }

    /// Dynamic — colours / types the `source`'s mana abilities
    /// *could* produce when the ability resolves (CR 106.7 future-
    /// snapshot of mana abilities). Reflecting Pool: "any type that
    /// a land you control could produce." Star Compass: "any colour
    /// that a basic land you control could produce." Squandered
    /// Resources: "any type the sacrificed land could produce." The
    /// `filter` discriminates "colour" (exclude `{C}`) from "type"
    /// (include `{C}`) per CR 106.7.
    record CouldProduce(ObjectSelector source, Filter filter) implements Palette {
        public CouldProduce {
            requireNonNull(source);
            requireNonNull(filter);
        }
    }

    /// Dynamic — colours / types `source` *actually* produced when
    /// last tapped for mana. Used by trigger-when-tapped abilities
    /// that mirror the produced type (Mirari's Wake, Sisay, Dictate
    /// of Karametra, Kinnan, Heartbeat of Spring's "that land
    /// produced"). Distinct from [CouldProduce]: a dual land tapped
    /// for `{U}` produced just `{U}`, not both halves. `filter`
    /// mirrors [CouldProduce#filter].
    record Produced(ObjectSelector source, Filter filter) implements Palette {
        public Produced {
            requireNonNull(source);
            requireNonNull(filter);
        }
    }

    /// Dynamic — colours *appearing on* a set of objects (Mox Amber:
    /// "any colour among legendary creatures and planeswalkers you
    /// control"). Always colour, never type — the "appears on"
    /// relation only ranges over the five colours.
    record AmongColorsOf(ObjectSelector objects) implements Palette {
        public AmongColorsOf {
            requireNonNull(objects);
        }
    }

    /// "Colour" vs "type" filter on a dynamic palette ({@mtg.rule
    /// 106.7}): `COLOR` restricts to the five basic colours (excludes
    /// `{C}`); `TYPE` includes the six basic types (W, U, B, R, G,
    /// C).
    enum Filter {
        COLOR,
        TYPE
    }
}
