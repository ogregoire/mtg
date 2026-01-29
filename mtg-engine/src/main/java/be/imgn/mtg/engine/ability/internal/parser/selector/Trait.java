package be.imgn.mtg.engine.ability.internal.parser.selector;

import be.imgn.mtg.engine.characteristics.Color;
import be.imgn.mtg.engine.characteristics.Supertype;
import be.imgn.mtg.engine.characteristics.Type;
import be.imgn.mtg.engine.object.Permanent;
import be.imgn.mtg.engine.object.Token;
import be.imgn.mtg.engine.object.TypedObject;

/// A characteristic trait that can be positively or negatively matched against a game object.
///
/// Used by [Qualifier.Has] and [Qualifier.Not] to express requirements like
/// "legendary" (Has) or "nonland" (Not).
public sealed interface Trait {

    /// Tests whether the given game object has this trait.
    boolean test(TypedObject object);

    /// A card type trait (creature, artifact, land, etc.).
    record CardType(Type type) implements Trait {
        @Override
        public boolean test(TypedObject object) {
            return object.types().contains(type);
        }
    }

    /// A color trait (white, blue, black, red, green).
    record ObjectColor(Color color) implements Trait {
        @Override
        public boolean test(TypedObject object) {
            return object.colors().contains(color);
        }
    }

    /// A supertype trait (legendary, basic, snow, world).
    record ObjectSupertype(Supertype supertype) implements Trait {
        @Override
        public boolean test(TypedObject object) {
            return object.supertypes().contains(supertype);
        }
    }

    /// A token source trait — the object is a token permanent.
    record TokenSource() implements Trait {
        @Override
        public boolean test(TypedObject object) {
            return object instanceof Permanent perm && perm.source() instanceof Token;
        }
    }
}
