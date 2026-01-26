package be.imgn.mtg.engine.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SetMultimap")
class SetMultimapTest {

    enum Category {
        FRUIT,
        VEGETABLE,
        MEAT
    }

    @Nested
    @DisplayName("HashSetMultimap")
    class HashSetMultimapTests {

        @Test
        void createEmpty() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();

            assertThat(multimap.isEmpty()).isTrue();
            assertThat(multimap.size()).isZero();
        }

        @Test
        void putSingleValue() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();

            multimap.put("key", 1);

            assertThat(multimap.get("key")).containsExactly(1);
            assertThat(multimap.size()).isEqualTo(1);
        }

        @Test
        void putDuplicateValueIsIgnored() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);
            multimap.put("key", 1);

            assertThat(multimap.get("key")).containsExactly(1);
            assertThat(multimap.size()).isEqualTo(1);
        }

        @Test
        void putMultipleDistinctValues() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);
            multimap.put("key", 3);

            assertThat(multimap.get("key")).containsExactlyInAnyOrder(1, 2, 3);
            assertThat(multimap.size()).isEqualTo(3);
        }

        @Test
        void getReturnsEmptySetForMissingKey() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();

            assertThat(multimap.get("missing")).isEmpty();
        }

        @Test
        void containsKey() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("present", 1);

            assertThat(multimap.containsKey("present")).isTrue();
            assertThat(multimap.containsKey("missing")).isFalse();
        }

        @Test
        void containsValue() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);

            assertThat(multimap.containsValue(1)).isTrue();
            assertThat(multimap.containsValue(2)).isFalse();
        }

        @Test
        void removeValue() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);

            var removed = multimap.remove("key", 1);

            assertThat(removed).isTrue();
            assertThat(multimap.get("key")).containsExactly(2);
        }

        @Test
        void removeNonExistentValue() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();

            var removed = multimap.remove("key", 1);

            assertThat(removed).isFalse();
        }

        @Test
        void removeAll() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);

            var removed = multimap.removeAll("key");

            assertThat(removed).containsExactlyInAnyOrder(1, 2);
            assertThat(multimap.get("key")).isEmpty();
        }

        @Test
        void clear() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key1", 1);
            multimap.put("key2", 2);

            multimap.clear();

            assertThat(multimap.isEmpty()).isTrue();
        }

        @Test
        void keySet() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("a", 1);
            multimap.put("b", 2);

            assertThat(multimap.keySet()).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        void values() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("a", 1);
            multimap.put("a", 2);
            multimap.put("b", 3);

            assertThat(multimap.values()).containsExactlyInAnyOrder(1, 2, 3);
        }

        @Test
        void asMap() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("a", 1);
            multimap.put("a", 2);
            multimap.put("b", 3);

            var map = multimap.asMap();

            assertThat(map.get("a")).containsExactlyInAnyOrder(1, 2);
            assertThat(map.get("b")).containsExactly(3);
        }

        @Test
        void equalsAndHashCode() {
            var multimap1 = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap1.put("a", 1);
            var multimap2 = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap2.put("a", 1);

            assertThat(multimap1).isEqualTo(multimap2);
            assertThat(multimap1.hashCode()).isEqualTo(multimap2.hashCode());
        }

        @Test
        void toStringFormat() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);

            var str = multimap.toString();

            assertThat(str).contains("key");
            assertThat(str).contains("1");
        }

        @Test
        void putReturnsTrue() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();

            var result = multimap.put("key", 1);

            assertThat(result).isTrue();
        }

        @Test
        void putDuplicateReturnsFalse() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);

            var result = multimap.put("key", 1);

            assertThat(result).isFalse();
        }

        @Test
        void removeLastValueRemovesKey() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);

            multimap.remove("key", 1);

            assertThat(multimap.containsKey("key")).isFalse();
            assertThat(multimap.keySet()).isEmpty();
        }

        @Test
        void removeAllForNonExistentKeyReturnsEmptySet() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();

            var removed = multimap.removeAll("nonexistent");

            assertThat(removed).isEmpty();
        }

        @Test
        void getReturnsUnmodifiableSet() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);

            var set = multimap.get("key");

            assertThatThrownBy(() -> set.add(2)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void asMapReturnsUnmodifiableMap() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);

            var map = multimap.asMap();

            assertThatThrownBy(() -> map.put("newkey", Set.of(2))).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void keySetIsSnapshot() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);

            var keySet = multimap.keySet();
            multimap.put("newkey", 2);

            assertThat(keySet).doesNotContain("newkey");
        }

        @Test
        void sizeTracksAcrossMultipleKeys() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("a", 1);
            multimap.put("b", 2);
            multimap.put("c", 3);

            assertThat(multimap.size()).isEqualTo(3);
        }

        @Test
        void sizeDecrementsAfterRemove() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);

            multimap.remove("key", 1);

            assertThat(multimap.size()).isEqualTo(1);
        }

        @Test
        void sizeResetsToZeroAfterClear() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);

            multimap.clear();

            assertThat(multimap.size()).isZero();
        }

        @Test
        void containsValueAcrossMultipleKeys() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("a", 1);
            multimap.put("b", 1);

            assertThat(multimap.containsValue(1)).isTrue();
        }

        @Test
        void notEqualToDifferentMultimap() {
            var multimap1 = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap1.put("a", 1);
            var multimap2 = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap2.put("a", 2);

            assertThat(multimap1).isNotEqualTo(multimap2);
        }

        @Test
        void notEqualToNull() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("a", 1);

            assertThat(multimap).isNotEqualTo(null);
        }

        @Test
        void multipleKeysWithSameValue() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("a", 1);
            multimap.put("b", 1);
            multimap.put("c", 1);

            assertThat(multimap.size()).isEqualTo(3);
            assertThat(multimap.keySet()).containsExactlyInAnyOrder("a", "b", "c");
        }

        @Test
        void largeNumberOfValues() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            for (int i = 0; i < 1000; i++) {
                multimap.put("key", i);
            }

            assertThat(multimap.size()).isEqualTo(1000);
            assertThat(multimap.get("key")).hasSize(1000);
        }

        @Test
        void valuesReturnsAllValues() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("a", 1);
            multimap.put("a", 2);
            multimap.put("b", 3);
            multimap.put("c", 4);

            var values = multimap.values();

            assertThat(values).containsExactlyInAnyOrder(1, 2, 3, 4);
        }

        @Test
        void emptyKeySetAfterRemoveAll() {
            var multimap = SetMultimap.<String, Integer>newHashSetMultimap();
            multimap.put("key", 1);

            multimap.removeAll("key");

            assertThat(multimap.keySet()).isEmpty();
        }
    }

    @Nested
    @DisplayName("HashEnumSetMultimap")
    class HashEnumSetMultimapTests {

        @Test
        void createEmpty() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);

            assertThat(multimap.isEmpty()).isTrue();
        }

        @Test
        void putSingleValue() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);

            multimap.put("item", Category.FRUIT);

            assertThat(multimap.get("item")).containsExactly(Category.FRUIT);
        }

        @Test
        void putDuplicateValueIsIgnored() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);
            multimap.put("item", Category.FRUIT);

            assertThat(multimap.get("item")).containsExactly(Category.FRUIT);
            assertThat(multimap.size()).isEqualTo(1);
        }

        @Test
        void putMultipleEnumValues() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);
            multimap.put("item", Category.VEGETABLE);

            assertThat(multimap.get("item")).containsExactlyInAnyOrder(Category.FRUIT, Category.VEGETABLE);
        }

        @Test
        void removeValue() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);
            multimap.put("item", Category.VEGETABLE);

            multimap.remove("item", Category.FRUIT);

            assertThat(multimap.get("item")).containsExactly(Category.VEGETABLE);
        }

        @Test
        void removeAll() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);
            multimap.put("item", Category.VEGETABLE);

            var removed = multimap.removeAll("item");

            assertThat(removed).containsExactlyInAnyOrder(Category.FRUIT, Category.VEGETABLE);
            assertThat(multimap.get("item")).isEmpty();
        }

        @Test
        void clear() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("a", Category.FRUIT);
            multimap.put("b", Category.MEAT);

            multimap.clear();

            assertThat(multimap.isEmpty()).isTrue();
        }

        @Test
        void keySet() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("a", Category.FRUIT);
            multimap.put("b", Category.MEAT);

            assertThat(multimap.keySet()).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        void values() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("a", Category.FRUIT);
            multimap.put("b", Category.MEAT);

            assertThat(multimap.values()).containsExactlyInAnyOrder(Category.FRUIT, Category.MEAT);
        }

        @Test
        void putReturnsTrue() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);

            var result = multimap.put("item", Category.FRUIT);

            assertThat(result).isTrue();
        }

        @Test
        void putDuplicateReturnsFalse() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);

            var result = multimap.put("item", Category.FRUIT);

            assertThat(result).isFalse();
        }

        @Test
        void removeLastValueRemovesKey() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);

            multimap.remove("item", Category.FRUIT);

            assertThat(multimap.containsKey("item")).isFalse();
        }

        @Test
        void removeAllForNonExistentKeyReturnsEmptySet() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);

            var removed = multimap.removeAll("nonexistent");

            assertThat(removed).isEmpty();
        }

        @Test
        void containsKey() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("present", Category.FRUIT);

            assertThat(multimap.containsKey("present")).isTrue();
            assertThat(multimap.containsKey("missing")).isFalse();
        }

        @Test
        void containsValue() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);

            assertThat(multimap.containsValue(Category.FRUIT)).isTrue();
            assertThat(multimap.containsValue(Category.MEAT)).isFalse();
        }

        @Test
        void sizeTracksCorrectly() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);
            multimap.put("item", Category.VEGETABLE);

            assertThat(multimap.size()).isEqualTo(2);
        }

        @Test
        void asMap() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);
            multimap.put("item", Category.VEGETABLE);

            var map = multimap.asMap();

            assertThat(map.get("item")).containsExactlyInAnyOrder(Category.FRUIT, Category.VEGETABLE);
        }

        @Test
        void getReturnsUnmodifiableSet() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);

            var set = multimap.get("item");

            assertThatThrownBy(() -> set.add(Category.MEAT)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void equalsAndHashCode() {
            var multimap1 = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap1.put("a", Category.FRUIT);
            var multimap2 = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap2.put("a", Category.FRUIT);

            assertThat(multimap1).isEqualTo(multimap2);
            assertThat(multimap1.hashCode()).isEqualTo(multimap2.hashCode());
        }

        @Test
        void toStringFormat() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);

            var str = multimap.toString();

            assertThat(str).contains("item");
            assertThat(str).contains("FRUIT");
        }

        @Test
        void putAllEnumValues() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("all", Category.FRUIT);
            multimap.put("all", Category.VEGETABLE);
            multimap.put("all", Category.MEAT);

            assertThat(multimap.get("all"))
                    .containsExactlyInAnyOrder(Category.FRUIT, Category.VEGETABLE, Category.MEAT);
        }

        @Test
        void removeNonExistentValue() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("item", Category.FRUIT);

            var removed = multimap.remove("item", Category.MEAT);

            assertThat(removed).isFalse();
            assertThat(multimap.get("item")).containsExactly(Category.FRUIT);
        }

        @Test
        void getReturnsEmptySetForMissingKey() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);

            assertThat(multimap.get("missing")).isEmpty();
        }

        @Test
        void multipleKeysWithEnumValues() {
            var multimap = SetMultimap.<String, Category>newHashEnumSetMultimap(Category.class);
            multimap.put("a", Category.FRUIT);
            multimap.put("b", Category.VEGETABLE);
            multimap.put("c", Category.MEAT);

            assertThat(multimap.size()).isEqualTo(3);
            assertThat(multimap.keySet()).containsExactlyInAnyOrder("a", "b", "c");
        }
    }
}
