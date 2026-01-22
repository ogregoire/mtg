package be.imgn.mtg.engine.mana;

/// Represents the different ways a player can pay a mana symbol.
///
/// Used with the Choice framework to present payment options for:
/// - Hybrid mana (choose one of two colors)
/// - Phyrexian mana (pay mana or life)
/// - Mono-color hybrid (pay color or 2 generic)
/// - Colorless hybrid (pay colorless or colored)
/// - Hybrid Phyrexian (pay either color or life)
public sealed interface ManaPaymentOption {

    /// Pay with a specific color of mana.
    ///
    /// @param type the mana type to pay
    record PayMana(ManaType type) implements ManaPaymentOption {
        @Override
        public String toString() {
            return "Pay " + type.notation();
        }
    }

    /// Pay life instead of mana (Phyrexian mana).
    ///
    /// @param amount the amount of life to pay
    record PayLife(int amount) implements ManaPaymentOption {
        /// Standard Phyrexian life payment of 2 life.
        public static final PayLife TWO = new PayLife(2);

        @Override
        public String toString() {
            return "Pay " + amount + " life";
        }
    }

    /// Pay generic mana (for mono-color hybrid symbols like {2/W}).
    ///
    /// @param amount the amount of generic mana to pay
    record PayGeneric(int amount) implements ManaPaymentOption {
        /// Standard mono-color hybrid generic payment of 2 mana.
        public static final PayGeneric TWO = new PayGeneric(2);

        @Override
        public String toString() {
            return "Pay {" + amount + "}";
        }
    }
}
