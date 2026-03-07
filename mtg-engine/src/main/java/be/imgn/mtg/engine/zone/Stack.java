package be.imgn.mtg.engine.zone;

import java.util.List;
import java.util.Optional;

import be.imgn.mtg.engine.object.StackObject;

/// The stack zone - where spells and abilities wait to resolve ({@mtg.rule 405}).
///
/// The stack is a shared zone where spells and abilities exist while waiting to resolve.
/// Objects on the stack resolve in last-in, first-out (LIFO) order.
///
/// The stack holds [StackObject]s: spells and abilities waiting to resolve.
/// When the stack is empty and all players pass priority, the game moves forward.
///
/// @see StackObject
/// @see ZoneType#STACK
public non-sealed interface Stack extends Zone<StackObject> {

    /// Pushes an object onto the stack.
    ///
    /// @param object the spell or ability to push
    void push(StackObject object);

    /// Returns the top object on the stack without removing it.
    ///
    /// @return the top object, or empty if the stack is empty
    Optional<StackObject> peek();

    /// Removes and returns the top object from the stack.
    ///
    /// This is called when the top object resolves.
    ///
    /// @return the resolved object, or empty if the stack is empty
    Optional<StackObject> pop();

    /// Removes a specific object from the stack (e.g., when countered).
    ///
    /// @param object the object to remove
    /// @return true if the object was found and removed
    boolean remove(StackObject object);

    /// Resolves the top object on the stack ({@mtg.rule 608}).
    ///
    /// For permanent spells, the card enters the battlefield.
    /// For instant/sorcery spells, the card is put into the graveyard.
    /// For abilities, they simply cease to exist.
    void resolve();

    /// Returns all objects on the stack in order (top to bottom).
    ///
    /// @return the stack contents
    List<StackObject> all();

    @Override
    default ZoneType type() {
        return ZoneType.STACK;
    }
}
