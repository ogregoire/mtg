package be.imgn.mtg.engine.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ListMultimapTest {

    enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }

    @Nested
    class EnumListMultimapTests {

        @Test
        void createEmpty() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            assertThat(multimap.isEmpty()).isTrue();
            assertThat(multimap.size()).isZero();
        }

        @Test
        void putSingleValue() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            multimap.put(Priority.HIGH, "task1");

            assertThat(multimap.get(Priority.HIGH)).containsExactly("task1");
            assertThat(multimap.size()).isEqualTo(1);
        }

        @Test
        void putMultipleValuesForSameKey() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task2");

            assertThat(multimap.get(Priority.HIGH)).containsExactly("task1", "task2");
            assertThat(multimap.size()).isEqualTo(2);
        }

        @Test
        void putDuplicateValuesAreSeparate() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task1");

            assertThat(multimap.get(Priority.HIGH)).containsExactly("task1", "task1");
            assertThat(multimap.size()).isEqualTo(2);
        }

        @Test
        void getReturnsEmptyListForMissingKey() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            assertThat(multimap.get(Priority.LOW)).isEmpty();
        }

        @Test
        void containsKey() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");

            assertThat(multimap.containsKey(Priority.HIGH)).isTrue();
            assertThat(multimap.containsKey(Priority.LOW)).isFalse();
        }

        @Test
        void containsValue() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");

            assertThat(multimap.containsValue("task1")).isTrue();
            assertThat(multimap.containsValue("task2")).isFalse();
        }

        @Test
        void removeValue() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task2");

            var removed = multimap.remove(Priority.HIGH, "task1");

            assertThat(removed).isTrue();
            assertThat(multimap.get(Priority.HIGH)).containsExactly("task2");
        }

        @Test
        void removeNonExistentValue() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            var removed = multimap.remove(Priority.HIGH, "task1");

            assertThat(removed).isFalse();
        }

        @Test
        void removeAll() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task2");

            var removed = multimap.removeAll(Priority.HIGH);

            assertThat(removed).containsExactly("task1", "task2");
            assertThat(multimap.get(Priority.HIGH)).isEmpty();
        }

        @Test
        void removeFirst() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task2");

            var removed = multimap.removeFirst(Priority.HIGH);

            assertThat(removed).isEqualTo("task1");
            assertThat(multimap.get(Priority.HIGH)).containsExactly("task2");
        }

        @Test
        void removeFirstFromEmpty() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            var removed = multimap.removeFirst(Priority.HIGH);

            assertThat(removed).isNull();
        }

        @Test
        void removeLast() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task2");

            var removed = multimap.removeLast(Priority.HIGH);

            assertThat(removed).isEqualTo("task2");
            assertThat(multimap.get(Priority.HIGH)).containsExactly("task1");
        }

        @Test
        void removeLastFromEmpty() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            var removed = multimap.removeLast(Priority.HIGH);

            assertThat(removed).isNull();
        }

        @Test
        void getFirst() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task2");

            assertThat(multimap.getFirst(Priority.HIGH)).isEqualTo("task1");
        }

        @Test
        void getFirstFromEmpty() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            assertThat(multimap.getFirst(Priority.HIGH)).isNull();
        }

        @Test
        void getLast() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task2");

            assertThat(multimap.getLast(Priority.HIGH)).isEqualTo("task2");
        }

        @Test
        void getLastFromEmpty() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            assertThat(multimap.getLast(Priority.HIGH)).isNull();
        }

        @Test
        void clear() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.LOW, "task2");

            multimap.clear();

            assertThat(multimap.isEmpty()).isTrue();
        }

        @Test
        void keySet() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.LOW, "task2");

            assertThat(multimap.keySet()).containsExactlyInAnyOrder(Priority.HIGH, Priority.LOW);
        }

        @Test
        void values() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task2");
            multimap.put(Priority.LOW, "task3");

            assertThat(multimap.values()).containsExactlyInAnyOrder("task1", "task2", "task3");
        }

        @Test
        void asMap() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            multimap.put(Priority.HIGH, "task2");
            multimap.put(Priority.LOW, "task3");

            var map = multimap.asMap();

            assertThat(map.get(Priority.HIGH)).containsExactly("task1", "task2");
            assertThat(map.get(Priority.LOW)).containsExactly("task3");
        }

        @Test
        void equalsAndHashCode() {
            var multimap1 = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap1.put(Priority.HIGH, "task1");
            var multimap2 = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap2.put(Priority.HIGH, "task1");

            assertThat(multimap1).isEqualTo(multimap2);
            assertThat(multimap1.hashCode()).isEqualTo(multimap2.hashCode());
        }

        @Test
        void toStringFormat() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");

            assertThat(multimap.toString()).contains("HIGH");
            assertThat(multimap.toString()).contains("task1");
        }

        @Test
        void removeLastValueRemovesKey() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");

            multimap.remove(Priority.HIGH, "task1");

            assertThat(multimap.containsKey(Priority.HIGH)).isFalse();
            assertThat(multimap.get(Priority.HIGH)).isEmpty();
        }

        @Test
        void removeFirstRemovesKeyWhenEmpty() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");

            multimap.removeFirst(Priority.HIGH);

            assertThat(multimap.containsKey(Priority.HIGH)).isFalse();
        }

        @Test
        void removeLastRemovesKeyWhenEmpty() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");

            multimap.removeLast(Priority.HIGH);

            assertThat(multimap.containsKey(Priority.HIGH)).isFalse();
        }

        @Test
        void getFirstFromNullList() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            assertThat(multimap.getFirst(Priority.HIGH)).isNull();
        }

        @Test
        void getLastFromNullList() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            assertThat(multimap.getLast(Priority.HIGH)).isNull();
        }

        @Test
        void removeFromNullList() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            var removed = multimap.remove(Priority.HIGH, "task1");

            assertThat(removed).isFalse();
        }

        @Test
        void removeFirstFromNullList() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            assertThat(multimap.removeFirst(Priority.HIGH)).isNull();
        }

        @Test
        void removeLastFromNullList() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            assertThat(multimap.removeLast(Priority.HIGH)).isNull();
        }

        @Test
        void removeAllFromNullList() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);

            var removed = multimap.removeAll(Priority.HIGH);

            assertThat(removed).isEmpty();
        }

        @Test
        void valuesSkipsNullLists() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            // Priority.LOW and MEDIUM have null lists

            assertThat(multimap.values()).containsExactly("task1");
        }

        @Test
        void keySetSkipsNullLists() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");
            // Priority.LOW and MEDIUM have null lists

            assertThat(multimap.keySet()).containsExactly(Priority.HIGH);
        }

        @Test
        void asMapSkipsNullLists() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");

            var map = multimap.asMap();

            assertThat(map).containsOnlyKeys(Priority.HIGH);
        }

        @Test
        void equalsWithDifferentContent() {
            var multimap1 = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap1.put(Priority.HIGH, "task1");
            var multimap2 = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap2.put(Priority.HIGH, "task2");

            assertThat(multimap1).isNotEqualTo(multimap2);
        }

        @Test
        void equalsWithNonMultimap() {
            var multimap = ListMultimap.<Priority, String>newEnumListMultimap(Priority.class);
            multimap.put(Priority.HIGH, "task1");

            assertThat(multimap).isNotEqualTo("not a multimap");
        }
    }

    @Nested
    class HashListMultimapTests {

        @Test
        void createEmpty() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();

            assertThat(multimap.isEmpty()).isTrue();
        }

        @Test
        void putAndGet() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("numbers", 1);
            multimap.put("numbers", 2);

            assertThat(multimap.get("numbers")).containsExactly(1, 2);
        }

        @Test
        void removeFirst() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("numbers", 1);
            multimap.put("numbers", 2);
            multimap.put("numbers", 3);

            assertThat(multimap.removeFirst("numbers")).isEqualTo(1);
            assertThat(multimap.get("numbers")).containsExactly(2, 3);
        }

        @Test
        void removeLast() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("numbers", 1);
            multimap.put("numbers", 2);
            multimap.put("numbers", 3);

            assertThat(multimap.removeLast("numbers")).isEqualTo(3);
            assertThat(multimap.get("numbers")).containsExactly(1, 2);
        }

        @Test
        void getFirst() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("numbers", 1);
            multimap.put("numbers", 2);

            assertThat(multimap.getFirst("numbers")).isEqualTo(1);
        }

        @Test
        void getLast() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("numbers", 1);
            multimap.put("numbers", 2);

            assertThat(multimap.getLast("numbers")).isEqualTo(2);
        }

        @Test
        void getFirstFromEmpty() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();

            assertThat(multimap.getFirst("missing")).isNull();
        }

        @Test
        void getLastFromEmpty() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();

            assertThat(multimap.getLast("missing")).isNull();
        }

        @Test
        void removeFirstFromEmpty() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();

            assertThat(multimap.removeFirst("missing")).isNull();
        }

        @Test
        void removeLastFromEmpty() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();

            assertThat(multimap.removeLast("missing")).isNull();
        }

        @Test
        void removeFirstRemovesKeyWhenEmpty() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("key", 1);

            multimap.removeFirst("key");

            assertThat(multimap.containsKey("key")).isFalse();
        }

        @Test
        void removeLastRemovesKeyWhenEmpty() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("key", 1);

            multimap.removeLast("key");

            assertThat(multimap.containsKey("key")).isFalse();
        }

        @Test
        void removeValueRemovesKeyWhenEmpty() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("key", 1);

            multimap.remove("key", 1);

            assertThat(multimap.containsKey("key")).isFalse();
        }

        @Test
        void removeAllFromEmpty() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();

            var removed = multimap.removeAll("missing");

            assertThat(removed).isEmpty();
        }

        @Test
        void containsKey() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("key", 1);

            assertThat(multimap.containsKey("key")).isTrue();
            assertThat(multimap.containsKey("missing")).isFalse();
        }

        @Test
        void containsValue() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("key", 1);

            assertThat(multimap.containsValue(1)).isTrue();
            assertThat(multimap.containsValue(999)).isFalse();
        }

        @Test
        void clear() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);

            multimap.clear();

            assertThat(multimap.isEmpty()).isTrue();
        }

        @Test
        void size() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("a", 1);
            multimap.put("a", 2);
            multimap.put("b", 3);

            assertThat(multimap.size()).isEqualTo(3);
        }

        @Test
        void keySet() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("a", 1);
            multimap.put("b", 2);

            assertThat(multimap.keySet()).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        void values() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("a", 1);
            multimap.put("a", 2);
            multimap.put("b", 3);

            assertThat(multimap.values()).containsExactlyInAnyOrder(1, 2, 3);
        }

        @Test
        void asMap() {
            var multimap = ListMultimap.<String, Integer>newHashListMultimap();
            multimap.put("a", 1);
            multimap.put("a", 2);

            var map = multimap.asMap();

            assertThat(map.get("a")).containsExactly(1, 2);
        }

        @Test
        void equalsAndHashCode() {
            var multimap1 = ListMultimap.<String, Integer>newHashListMultimap();
            multimap1.put("key", 1);
            var multimap2 = ListMultimap.<String, Integer>newHashListMultimap();
            multimap2.put("key", 1);

            assertThat(multimap1).isEqualTo(multimap2);
            assertThat(multimap1.hashCode()).isEqualTo(multimap2.hashCode());
        }
    }

    @Nested
    class TreeListMultimapTests {

        @Test
        void createEmptyWithNaturalOrder() {
            var multimap = ListMultimap.<String, Integer>newTreeListMultimap();

            assertThat(multimap.isEmpty()).isTrue();
        }

        @Test
        void keysAreSortedInAsMap() {
            var multimap = ListMultimap.<String, Integer>newTreeListMultimap();
            multimap.put("c", 3);
            multimap.put("a", 1);
            multimap.put("b", 2);

            // asMap() preserves TreeMap ordering, keySet() uses Set.copyOf() which doesn't
            assertThat(multimap.asMap().keySet()).containsExactly("a", "b", "c");
        }

        @Test
        void createWithComparator() {
            var multimap = ListMultimap.<String, Integer>newTreeListMultimap(Comparator.reverseOrder());
            multimap.put("c", 3);
            multimap.put("a", 1);
            multimap.put("b", 2);

            // asMap() preserves TreeMap ordering with custom comparator
            assertThat(multimap.asMap().keySet()).containsExactly("c", "b", "a");
        }

        @Test
        void putAndGet() {
            var multimap = ListMultimap.<String, Integer>newTreeListMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);

            assertThat(multimap.get("key")).containsExactly(1, 2);
        }

        @Test
        void removeFirst() {
            var multimap = ListMultimap.<String, Integer>newTreeListMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);
            multimap.put("key", 3);

            assertThat(multimap.removeFirst("key")).isEqualTo(1);
        }

        @Test
        void removeLast() {
            var multimap = ListMultimap.<String, Integer>newTreeListMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);
            multimap.put("key", 3);

            assertThat(multimap.removeLast("key")).isEqualTo(3);
        }

        @Test
        void getFirst() {
            var multimap = ListMultimap.<String, Integer>newTreeListMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);

            assertThat(multimap.getFirst("key")).isEqualTo(1);
        }

        @Test
        void getLast() {
            var multimap = ListMultimap.<String, Integer>newTreeListMultimap();
            multimap.put("key", 1);
            multimap.put("key", 2);

            assertThat(multimap.getLast("key")).isEqualTo(2);
        }
    }
}
