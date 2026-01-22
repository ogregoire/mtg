package be.imgn.mtg.engine.game;

/// Specifies how many options must be selected from a Choice.
///
/// Used to validate player selections:
/// - Exactly(n): must select exactly n options
/// - UpTo(max): can select 0 to max options
/// - AtLeast(min): must select at least min options
/// - Between(min, max): must select between min and max options
public sealed interface SelectionCount {

    /// Checks if the given number of selected options is valid.
    ///
    /// @param selected the number of options selected
    /// @return true if the selection count is valid
    boolean isValid(int selected);

    /// Requires exactly n options to be selected.
    ///
    /// @param n the exact number of options that must be selected
    record Exactly(int n) implements SelectionCount {
        /// Creates a new Exactly constraint.
        public Exactly {
            if (n < 0) {
                throw new IllegalArgumentException("n must be non-negative: " + n);
            }
        }

        @Override
        public boolean isValid(int selected) {
            return selected == n;
        }

        @Override
        public String toString() {
            return "exactly " + n;
        }
    }

    /// Allows between min and max options to be selected.
    ///
    /// @param min the minimum number of options
    /// @param max the maximum number of options
    record Between(int min, int max) implements SelectionCount {
        /// Creates a new Between constraint.
        public Between {
            if (min < 0) {
                throw new IllegalArgumentException("min must be non-negative: " + min);
            }
            if (max < min) {
                throw new IllegalArgumentException("max must be >= min: " + max + " < " + min);
            }
        }

        @Override
        public boolean isValid(int selected) {
            return selected >= min && selected <= max;
        }

        @Override
        public String toString() {
            return "between " + min + " and " + max;
        }
    }

    /// Allows 0 to max options to be selected.
    ///
    /// @param max the maximum number of options
    record UpTo(int max) implements SelectionCount {
        /// Creates a new UpTo constraint.
        public UpTo {
            if (max < 0) {
                throw new IllegalArgumentException("max must be non-negative: " + max);
            }
        }

        @Override
        public boolean isValid(int selected) {
            return selected >= 0 && selected <= max;
        }

        @Override
        public String toString() {
            return "up to " + max;
        }
    }

    /// Requires at least min options to be selected.
    ///
    /// @param min the minimum number of options
    record AtLeast(int min) implements SelectionCount {
        /// Creates a new AtLeast constraint.
        public AtLeast {
            if (min < 0) {
                throw new IllegalArgumentException("min must be non-negative: " + min);
            }
        }

        @Override
        public boolean isValid(int selected) {
            return selected >= min;
        }

        @Override
        public String toString() {
            return "at least " + min;
        }
    }

    /// Creates a SelectionCount that requires exactly n options.
    ///
    /// @param n the exact number of options
    /// @return a new SelectionCount
    static SelectionCount exactly(int n) {
        return new Exactly(n);
    }

    /// Creates a SelectionCount that allows up to max options.
    ///
    /// @param max the maximum number of options
    /// @return a new SelectionCount
    static SelectionCount upTo(int max) {
        return new UpTo(max);
    }

    /// Creates a SelectionCount that requires at least min options.
    ///
    /// @param min the minimum number of options
    /// @return a new SelectionCount
    static SelectionCount atLeast(int min) {
        return new AtLeast(min);
    }

    /// Creates a SelectionCount that allows between min and max options.
    ///
    /// @param min the minimum number of options
    /// @param max the maximum number of options
    /// @return a new SelectionCount
    static SelectionCount between(int min, int max) {
        return new Between(min, max);
    }
}
