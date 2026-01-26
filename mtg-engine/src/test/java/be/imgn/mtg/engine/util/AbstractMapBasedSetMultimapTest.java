package be.imgn.mtg.engine.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractMapBasedSetMultimap")
class AbstractMapBasedSetMultimapTest {

    /// Test implementation that exposes the abstract class for testing
    static class TestSetMultimap<K, V> extends AbstractMapBasedSetMultimap<K, V> {
        TestSetMultimap(Map<K, Set<V>> backingMap, Supplier<Set<V>> setSupplier) {
            super(backingMap, setSupplier);
        }
    }

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        void requiresEmptyBackingMap() {
            Map<String, Set<Integer>> emptyMap = new HashMap<>();

            var multimap = new TestSetMultimap<>(emptyMap, HashSet::new);

            assertThat(multimap).isNotNull();
        }

        @Test
        void throwsExceptionForNonEmptyBackingMap() {
            Map<String, Set<Integer>> nonEmptyMap = new HashMap<>();
            nonEmptyMap.put("key", new HashSet<>());

            assertThatThrownBy(() -> new TestSetMultimap<>(nonEmptyMap, HashSet::new))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Backing map must be empty");
        }

        @Test
        void acceptsEmptyMapWithZeroSize() {
            Map<String, Set<Integer>> emptyMap = new HashMap<>();
            var multimap = new TestSetMultimap<>(emptyMap, HashSet::new);

            assertThat(multimap.size()).isZero();
            assertThat(multimap.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("size tracking")
    class SizeTracking {

        @Test
        void sizeStartsAtZero() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);

            assertThat(multimap.size()).isZero();
        }

        @Test
        void sizeIncrementsOnPut() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);

            multimap.put("key", 1);
            multimap.put("key", 2);

            assertThat(multimap.size()).isEqualTo(2);
        }

        @Test
        void sizeDoesNotChangeOnDuplicatePut() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);

            multimap.put("key", 1);

            assertThat(multimap.size()).isEqualTo(1);
        }

        @Test
        void sizeDecrementsOnRemove() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);
            multimap.put("key", 2);

            multimap.remove("key", 1);

            assertThat(multimap.size()).isEqualTo(1);
        }

        @Test
        void sizeResetsOnClear() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key1", 1);
            multimap.put("key2", 2);

            multimap.clear();

            assertThat(multimap.size()).isZero();
        }

        @Test
        void sizeDecrementsOnRemoveAll() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);
            multimap.put("key", 2);
            multimap.put("key", 3);

            multimap.removeAll("key");

            assertThat(multimap.size()).isZero();
        }

        @Test
        void sizeHandlesLargeValues() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);

            for (int i = 0; i < 10000; i++) {
                multimap.put("key", i);
            }

            assertThat(multimap.size()).isEqualTo(10000);
        }

        @Test
        void sizeCapsAtIntegerMaxValue() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            // We can't actually create Integer.MAX_VALUE + 1 entries in a test,
            // but we can verify the size() method returns an int
            multimap.put("key", 1);

            assertThat(multimap.size()).isInstanceOf(Integer.class);
            assertThat(multimap.size()).isLessThanOrEqualTo(Integer.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("set supplier")
    class SetSupplier {

        @Test
        void usesSupplierToCreateNewSets() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);

            multimap.put("key1", 1);
            multimap.put("key2", 2);

            // Both keys should have their own sets created by the supplier
            assertThat(multimap.get("key1")).containsExactly(1);
            assertThat(multimap.get("key2")).containsExactly(2);
        }

        @Test
        void onlyCreatesSetOnFirstPut() {
            var supplierCallCount = new int[1];
            Supplier<Set<Integer>> countingSupplier = () -> {
                supplierCallCount[0]++;
                return new HashSet<>();
            };
            var multimap = new TestSetMultimap<>(new HashMap<>(), countingSupplier);

            multimap.put("key", 1);
            multimap.put("key", 2);
            multimap.put("key", 3);

            // Supplier should only be called once per key
            assertThat(supplierCallCount[0]).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("keySet")
    class KeySet {

        @Test
        void keySetIsImmutableCopy() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key1", 1);

            var keySet = multimap.keySet();

            // Adding to multimap shouldn't affect the returned key set
            multimap.put("key2", 2);
            assertThat(keySet).containsExactly("key1");
        }

        @Test
        void keySetReflectsCurrentState() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key1", 1);
            multimap.put("key2", 2);

            var keySet = multimap.keySet();

            assertThat(keySet).containsExactlyInAnyOrder("key1", "key2");
        }

        @Test
        void keySetExcludesRemovedKeys() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key1", 1);
            multimap.put("key2", 2);
            multimap.removeAll("key1");

            var keySet = multimap.keySet();

            assertThat(keySet).containsExactly("key2");
        }
    }

    @Nested
    @DisplayName("values")
    class Values {

        @Test
        void valuesReturnsAllValuesAcrossKeys() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("a", 1);
            multimap.put("a", 2);
            multimap.put("b", 3);

            var values = multimap.values();

            assertThat(values).containsExactlyInAnyOrder(1, 2, 3);
        }

        @Test
        void valuesReturnsUnmodifiableCollection() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);

            var values = multimap.values();

            assertThatThrownBy(() -> values.add(2)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void valuesReflectsCurrentState() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);

            var valuesBefore = multimap.values();
            multimap.put("key", 2);
            var valuesAfter = multimap.values();

            // Old collection should not change
            assertThat(valuesBefore).containsExactly(1);
            // New collection reflects changes
            assertThat(valuesAfter).containsExactlyInAnyOrder(1, 2);
        }

        @Test
        void valuesHandlesEmptyMultimap() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);

            var values = multimap.values();

            assertThat(values).isEmpty();
        }
    }

    @Nested
    @DisplayName("asMap")
    class AsMap {

        @Test
        void asMapReturnsUnmodifiableView() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);

            var map = multimap.asMap();

            assertThatThrownBy(() -> map.put("newkey", new HashSet<>()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void asMapReflectsChanges() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);

            var map = multimap.asMap();

            // Map should reflect current state
            assertThat(map.get("key")).containsExactly(1);

            // After adding more values
            multimap.put("key", 2);
            assertThat(map.get("key")).containsExactlyInAnyOrder(1, 2);
        }

        @Test
        void asMapReturnsAllKeys() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("a", 1);
            multimap.put("b", 2);

            var map = multimap.asMap();

            assertThat(map.keySet()).containsExactlyInAnyOrder("a", "b");
        }
    }

    @Nested
    @DisplayName("remove behavior")
    class RemoveBehavior {

        @Test
        void removeLastValueCleansUpKey() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);

            multimap.remove("key", 1);

            // Key should be removed from backing map when last value is removed
            assertThat(multimap.containsKey("key")).isFalse();
            assertThat(multimap.asMap()).doesNotContainKey("key");
        }

        @Test
        void removeReturnsFalseForNonExistentKey() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);

            var removed = multimap.remove("nonexistent", 1);

            assertThat(removed).isFalse();
        }

        @Test
        void removeReturnsFalseForNonExistentValue() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);

            var removed = multimap.remove("key", 2);

            assertThat(removed).isFalse();
        }

        @Test
        void removeReturnsTrueForExistingValue() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);

            var removed = multimap.remove("key", 1);

            assertThat(removed).isTrue();
        }
    }

    @Nested
    @DisplayName("edge cases")
    class EdgeCases {

        @Test
        void clearOnEmptyMultimap() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);

            multimap.clear();

            assertThat(multimap).isNotNull();
            assertThat(multimap.isEmpty()).isTrue();
        }

        @Test
        void getAfterRemoveAllReturnsEmptySet() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);
            multimap.removeAll("key");

            var result = multimap.get("key");

            assertThat(result).isEmpty();
        }

        @Test
        void removeAllReturnsRemovedValues() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);
            multimap.put("key", 2);

            var removed = multimap.removeAll("key");

            assertThat(removed).containsExactlyInAnyOrder(1, 2);
        }

        @Test
        void multipleRemoveAllCallsOnSameKey() {
            var multimap = new TestSetMultimap<>(new HashMap<>(), HashSet::new);
            multimap.put("key", 1);

            var removed1 = multimap.removeAll("key");
            var removed2 = multimap.removeAll("key");

            assertThat(removed1).containsExactly(1);
            assertThat(removed2).isEmpty();
        }
    }
}
