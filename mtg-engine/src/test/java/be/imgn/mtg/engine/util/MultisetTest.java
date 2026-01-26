package be.imgn.mtg.engine.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MultisetTest {

    enum Color {
        RED,
        GREEN,
        BLUE
    }

    @Nested
    class EnumMultisetTests {

        @Test
        void createEmpty() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            assertThat(multiset).isEmpty();
            assertThat(multiset.size()).isZero();
        }

        @Test
        void addSingleElement() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            var oldCount = multiset.add(Color.RED, 1);

            assertThat(oldCount).isZero();
            assertThat(multiset.count(Color.RED)).isEqualTo(1);
            assertThat(multiset.size()).isEqualTo(1);
        }

        @Test
        void addMultipleOccurrences() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            multiset.add(Color.RED, 3);

            assertThat(multiset.count(Color.RED)).isEqualTo(3);
            assertThat(multiset.size()).isEqualTo(3);
        }

        @Test
        void addZeroOccurrences() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var oldCount = multiset.add(Color.RED, 0);

            assertThat(oldCount).isEqualTo(2);
            assertThat(multiset.count(Color.RED)).isEqualTo(2);
        }

        @Test
        void addNegativeOccurrencesThrows() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            assertThatThrownBy(() -> multiset.add(Color.RED, -1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void removeOccurrences() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 5);

            var oldCount = multiset.remove(Color.RED, 2);

            assertThat(oldCount).isEqualTo(5);
            assertThat(multiset.count(Color.RED)).isEqualTo(3);
        }

        @Test
        void removeAllOccurrences() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);

            multiset.remove(Color.RED, 5);

            assertThat(multiset.count(Color.RED)).isZero();
            assertThat(multiset).isEmpty();
        }

        @Test
        void removeFromNonExistentElement() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            var oldCount = multiset.remove(Color.RED, 1);

            assertThat(oldCount).isZero();
        }

        @Test
        void removeZeroOccurrences() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var oldCount = multiset.remove(Color.RED, 0);

            assertThat(oldCount).isEqualTo(2);
        }

        @Test
        void removeNegativeOccurrencesThrows() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            assertThatThrownBy(() -> multiset.remove(Color.RED, -1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void setCount() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var oldCount = multiset.setCount(Color.RED, 5);

            assertThat(oldCount).isEqualTo(2);
            assertThat(multiset.count(Color.RED)).isEqualTo(5);
        }

        @Test
        void setCountToZero() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);

            multiset.setCount(Color.RED, 0);

            assertThat(multiset.count(Color.RED)).isZero();
            assertThat(multiset).isEmpty();
        }

        @Test
        void setCountNegativeThrows() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            assertThatThrownBy(() -> multiset.setCount(Color.RED, -1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void setCountConditionalSuccess() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var success = multiset.setCount(Color.RED, 2, 5);

            assertThat(success).isTrue();
            assertThat(multiset.count(Color.RED)).isEqualTo(5);
        }

        @Test
        void setCountConditionalFailure() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var success = multiset.setCount(Color.RED, 3, 5);

            assertThat(success).isFalse();
            assertThat(multiset.count(Color.RED)).isEqualTo(2);
        }

        @Test
        void countNull() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            assertThat(multiset.count(null)).isZero();
        }

        @Test
        void countWrongType() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            assertThat(multiset.count("not an enum")).isZero();
        }

        @Test
        void clear() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);
            multiset.add(Color.GREEN, 3);

            multiset.clear();

            assertThat(multiset).isEmpty();
            assertThat(multiset.count(Color.RED)).isZero();
        }

        @Test
        void elementSet() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);
            multiset.add(Color.GREEN, 1);

            var elements = multiset.elementSet();

            assertThat(elements).containsExactlyInAnyOrder(Color.RED, Color.GREEN);
        }

        @Test
        void entrySet() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);
            multiset.add(Color.GREEN, 3);

            var entries = multiset.entrySet();

            assertThat(entries).hasSize(2);
            for (var entry : entries) {
                if (entry.element() == Color.RED) {
                    assertThat(entry.count()).isEqualTo(2);
                } else {
                    assertThat(entry.count()).isEqualTo(3);
                }
            }
        }

        @Test
        void iteratorTraversesAllOccurrences() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);
            multiset.add(Color.GREEN, 1);

            var elements = new ArrayList<Color>();
            for (var color : multiset) {
                elements.add(color);
            }

            assertThat(elements).hasSize(3);
            assertThat(elements).contains(Color.RED, Color.RED, Color.GREEN);
        }

        @Test
        void iteratorRemove() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var iterator = multiset.iterator();
            iterator.next();
            iterator.remove();

            assertThat(multiset.count(Color.RED)).isEqualTo(1);
        }

        @Test
        void iteratorRemoveWithoutNext() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 1);

            var iterator = multiset.iterator();

            assertThatThrownBy(iterator::remove).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void iteratorRemoveTwice() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var iterator = multiset.iterator();
            iterator.next();
            iterator.remove();

            assertThatThrownBy(iterator::remove).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void iteratorNextOnEmpty() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            var iterator = multiset.iterator();

            assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void entryIteratorRemove() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);
            multiset.add(Color.GREEN, 1);

            var iterator = multiset.entrySet().iterator();
            iterator.next();
            iterator.remove();

            assertThat(multiset.entrySet()).hasSize(1);
        }

        @Test
        void forEachEntry() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);
            multiset.add(Color.GREEN, 3);

            var visited = new ArrayList<String>();
            multiset.forEachEntry((elem, count) -> visited.add(elem + ":" + count));

            assertThat(visited).containsExactlyInAnyOrder("RED:2", "GREEN:3");
        }

        @Test
        void forEach() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var visited = new ArrayList<Color>();
            multiset.forEach(visited::add);

            assertThat(visited).containsExactly(Color.RED, Color.RED);
        }

        @Test
        void contains() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 1);

            assertThat(multiset.contains(Color.RED)).isTrue();
            assertThat(multiset.contains(Color.GREEN)).isFalse();
        }

        @Test
        void addViaCollection() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            multiset.add(Color.RED);

            assertThat(multiset.count(Color.RED)).isEqualTo(1);
        }

        @Test
        void removeViaCollection() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);

            var removed = multiset.remove(Color.RED);

            assertThat(removed).isTrue();
            assertThat(multiset.count(Color.RED)).isEqualTo(2);
        }

        @Test
        void toArray() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var array = multiset.toArray();

            assertThat(array).hasSize(2);
        }

        @Test
        void toTypedArray() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var array = multiset.toArray(new Color[0]);

            assertThat(array).hasSize(2);
            assertThat(array).containsExactly(Color.RED, Color.RED);
        }

        @Test
        void containsAll() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 1);
            multiset.add(Color.GREEN, 1);

            assertThat(multiset.containsAll(List.of(Color.RED, Color.GREEN))).isTrue();
            assertThat(multiset.containsAll(List.of(Color.RED, Color.BLUE))).isFalse();
        }

        @Test
        void addAll() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            multiset.addAll(List.of(Color.RED, Color.RED, Color.GREEN));

            assertThat(multiset.count(Color.RED)).isEqualTo(2);
            assertThat(multiset.count(Color.GREEN)).isEqualTo(1);
        }

        @Test
        void removeAll() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);
            multiset.add(Color.GREEN, 2);

            multiset.removeAll(List.of(Color.RED));

            assertThat(multiset.count(Color.RED)).isZero();
            assertThat(multiset.count(Color.GREEN)).isEqualTo(2);
        }

        @Test
        void retainAll() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);
            multiset.add(Color.GREEN, 3);
            multiset.add(Color.BLUE, 1);

            multiset.retainAll(List.of(Color.RED, Color.GREEN));

            assertThat(multiset.count(Color.RED)).isEqualTo(2);
            assertThat(multiset.count(Color.GREEN)).isEqualTo(3);
            assertThat(multiset.count(Color.BLUE)).isZero();
        }

        @Test
        void equalsAndHashCode() {
            var multiset1 = Multiset.newEnumMultiset(Color.class);
            multiset1.add(Color.RED, 2);
            var multiset2 = Multiset.newEnumMultiset(Color.class);
            multiset2.add(Color.RED, 2);

            assertThat(multiset1).isEqualTo(multiset2);
            assertThat(multiset1.hashCode()).isEqualTo(multiset2.hashCode());
        }

        @Test
        void notEqualsWithDifferentCounts() {
            var multiset1 = Multiset.newEnumMultiset(Color.class);
            multiset1.add(Color.RED, 2);
            var multiset2 = Multiset.newEnumMultiset(Color.class);
            multiset2.add(Color.RED, 3);

            assertThat(multiset1).isNotEqualTo(multiset2);
        }

        @Test
        void toStringFormat() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var str = multiset.toString();

            assertThat(str).contains("RED");
        }

        @Test
        void setCountConditionalNegativeOldCountThrows() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            assertThatThrownBy(() -> multiset.setCount(Color.RED, -1, 5)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void setCountConditionalNegativeNewCountThrows() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            assertThatThrownBy(() -> multiset.setCount(Color.RED, 0, -1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void addAllEmptyCollectionReturnsFalse() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            var changed = multiset.addAll(List.of());

            assertThat(changed).isFalse();
        }

        @Test
        void addAllFromMultisetUsesOptimizedPath() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            var source = Multiset.newEnumMultiset(Color.class);
            source.add(Color.RED, 3);
            source.add(Color.GREEN, 2);

            multiset.addAll(source);

            assertThat(multiset.count(Color.RED)).isEqualTo(3);
            assertThat(multiset.count(Color.GREEN)).isEqualTo(2);
        }

        @Test
        void removeAllFromMultiset() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);
            multiset.add(Color.GREEN, 2);

            var toRemove = Multiset.newEnumMultiset(Color.class);
            toRemove.add(Color.RED, 1);

            multiset.removeAll(toRemove);

            assertThat(multiset.count(Color.RED)).isZero();
            assertThat(multiset.count(Color.GREEN)).isEqualTo(2);
        }

        @Test
        void retainAllFromMultiset() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);
            multiset.add(Color.GREEN, 2);
            multiset.add(Color.BLUE, 1);

            var toRetain = Multiset.newEnumMultiset(Color.class);
            toRetain.add(Color.RED, 1);
            toRetain.add(Color.GREEN, 1);

            multiset.retainAll(toRetain);

            assertThat(multiset.count(Color.RED)).isEqualTo(3);
            assertThat(multiset.count(Color.GREEN)).isEqualTo(2);
            assertThat(multiset.count(Color.BLUE)).isZero();
        }

        @Test
        void entrySetContainsEntryWithZeroCount() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var zeroEntry = new Multiset.Entry<Color>() {
                @Override
                public Color element() {
                    return Color.RED;
                }

                @Override
                public int count() {
                    return 0;
                }
            };

            assertThat(multiset.entrySet().contains(zeroEntry)).isFalse();
        }

        @Test
        void entrySetContainsEntryWithWrongCount() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var wrongEntry = new Multiset.Entry<Color>() {
                @Override
                public Color element() {
                    return Color.RED;
                }

                @Override
                public int count() {
                    return 5;
                }
            };

            assertThat(multiset.entrySet().contains(wrongEntry)).isFalse();
        }

        @Test
        void entrySetContainsMatchingEntry() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var matchingEntry = new Multiset.Entry<Color>() {
                @Override
                public Color element() {
                    return Color.RED;
                }

                @Override
                public int count() {
                    return 2;
                }
            };

            assertThat(multiset.entrySet().contains(matchingEntry)).isTrue();
        }

        @Test
        void entrySetRemoveEntryWithZeroCount() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var zeroEntry = new Multiset.Entry<Color>() {
                @Override
                public Color element() {
                    return Color.RED;
                }

                @Override
                public int count() {
                    return 0;
                }
            };

            var removed = multiset.entrySet().remove(zeroEntry);

            assertThat(removed).isFalse();
        }

        @Test
        void entrySetRemoveMatchingEntry() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var entry = multiset.entrySet().iterator().next();
            var removed = multiset.entrySet().remove(entry);

            assertThat(removed).isTrue();
            assertThat(multiset.count(Color.RED)).isZero();
        }

        @Test
        void entrySetClear() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            multiset.entrySet().clear();

            assertThat(multiset).isEmpty();
        }

        @Test
        void elementSetRemove() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);

            var removed = multiset.elementSet().remove(Color.RED);

            assertThat(removed).isTrue();
            assertThat(multiset.count(Color.RED)).isZero();
        }

        @Test
        void elementSetRemoveNonExistent() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            var removed = multiset.elementSet().remove(Color.RED);

            assertThat(removed).isFalse();
        }

        @Test
        void elementSetClear() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            multiset.elementSet().clear();

            assertThat(multiset).isEmpty();
        }

        @Test
        void elementSetContains() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            assertThat(multiset.elementSet().contains(Color.RED)).isTrue();
            assertThat(multiset.elementSet().contains(Color.GREEN)).isFalse();
        }

        @Test
        void elementSetContainsAll() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);
            multiset.add(Color.GREEN, 1);

            assertThat(multiset.elementSet().containsAll(List.of(Color.RED, Color.GREEN)))
                    .isTrue();
            assertThat(multiset.elementSet().containsAll(List.of(Color.RED, Color.BLUE)))
                    .isFalse();
        }

        @Test
        void elementSetIsEmpty() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            assertThat(multiset.elementSet().isEmpty()).isTrue();

            multiset.add(Color.RED, 1);

            assertThat(multiset.elementSet().isEmpty()).isFalse();
        }

        @Test
        void elementSetSize() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);
            multiset.add(Color.GREEN, 2);

            assertThat(multiset.elementSet().size()).isEqualTo(2);
        }

        @Test
        void equalsWithDifferentSize() {
            var multiset1 = Multiset.newEnumMultiset(Color.class);
            multiset1.add(Color.RED, 2);
            var multiset2 = Multiset.newEnumMultiset(Color.class);
            multiset2.add(Color.RED, 3);

            assertThat(multiset1).isNotEqualTo(multiset2);
        }

        @Test
        void equalsWithDifferentEntrySetSize() {
            var multiset1 = Multiset.newEnumMultiset(Color.class);
            multiset1.add(Color.RED, 2);
            var multiset2 = Multiset.newEnumMultiset(Color.class);
            multiset2.add(Color.RED, 1);
            multiset2.add(Color.GREEN, 1);

            assertThat(multiset1).isNotEqualTo(multiset2);
        }

        @Test
        void equalsWithNonMultiset() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            assertThat(multiset).isNotEqualTo("not a multiset");
        }

        @Test
        void entryToStringWithCountOne() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 1);

            var entry = multiset.entrySet().iterator().next();

            assertThat(entry.toString()).isEqualTo("RED");
        }

        @Test
        void entryToStringWithCountGreaterThanOne() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);

            var entry = multiset.entrySet().iterator().next();

            assertThat(entry.toString()).isEqualTo("RED x 3");
        }

        @Test
        void entryEqualsWithDifferentCount() {
            var multiset1 = Multiset.newEnumMultiset(Color.class);
            multiset1.add(Color.RED, 2);
            var multiset2 = Multiset.newEnumMultiset(Color.class);
            multiset2.add(Color.RED, 3);

            var entry1 = multiset1.entrySet().iterator().next();
            var entry2 = multiset2.entrySet().iterator().next();

            assertThat(entry1).isNotEqualTo(entry2);
        }

        @Test
        void entryEqualsWithDifferentElement() {
            var multiset1 = Multiset.newEnumMultiset(Color.class);
            multiset1.add(Color.RED, 2);
            var multiset2 = Multiset.newEnumMultiset(Color.class);
            multiset2.add(Color.GREEN, 2);

            var entry1 = multiset1.entrySet().iterator().next();
            var entry2 = multiset2.entrySet().iterator().next();

            assertThat(entry1).isNotEqualTo(entry2);
        }

        @Test
        void entryEqualsWithSameElementAndCount() {
            var multiset1 = Multiset.newEnumMultiset(Color.class);
            multiset1.add(Color.RED, 2);
            var multiset2 = Multiset.newEnumMultiset(Color.class);
            multiset2.add(Color.RED, 2);

            var entry1 = multiset1.entrySet().iterator().next();
            var entry2 = multiset2.entrySet().iterator().next();

            assertThat(entry1).isEqualTo(entry2);
            assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode());
        }

        @Test
        void setCountIncrease() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            multiset.setCount(Color.RED, 5);

            assertThat(multiset.count(Color.RED)).isEqualTo(5);
        }

        @Test
        void setCountDecrease() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 5);

            multiset.setCount(Color.RED, 2);

            assertThat(multiset.count(Color.RED)).isEqualTo(2);
        }

        @Test
        void setCountSameValue() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);

            var oldCount = multiset.setCount(Color.RED, 3);

            assertThat(oldCount).isEqualTo(3);
            assertThat(multiset.count(Color.RED)).isEqualTo(3);
        }

        @Test
        void iteratorRemoveLastElement() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 1);

            var iterator = multiset.iterator();
            iterator.next();
            iterator.remove();

            assertThat(multiset).isEmpty();
        }

        @Test
        void addTooManyOccurrencesThrows() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, Integer.MAX_VALUE);

            assertThatThrownBy(() -> multiset.add(Color.RED, 1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void setCountFromZeroToPositive() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            multiset.setCount(Color.RED, 5);

            assertThat(multiset.count(Color.RED)).isEqualTo(5);
            assertThat(multiset.entrySet()).hasSize(1);
        }

        @Test
        void setCountFromPositiveToZero() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 5);

            multiset.setCount(Color.RED, 0);

            assertThat(multiset.count(Color.RED)).isZero();
            assertThat(multiset.entrySet()).isEmpty();
        }

        @Test
        void setCountFromPositiveToPositive() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);

            multiset.setCount(Color.RED, 7);

            assertThat(multiset.count(Color.RED)).isEqualTo(7);
            assertThat(multiset.entrySet()).hasSize(1);
        }

        @Test
        void removePartialOccurrences() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 10);

            multiset.remove(Color.RED, 3);

            assertThat(multiset.count(Color.RED)).isEqualTo(7);
        }

        @Test
        void removeExactOccurrences() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 5);

            multiset.remove(Color.RED, 5);

            assertThat(multiset.count(Color.RED)).isZero();
            assertThat(multiset.entrySet()).isEmpty();
        }

        @Test
        void entryIteratorRemoveOnAlreadyRemoved() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 2);

            var iterator = multiset.entrySet().iterator();
            iterator.next();
            // Remove via multiset, not via iterator
            multiset.setCount(Color.RED, 0);
            // Now iterator.remove() should handle the case where count is already 0
            iterator.remove();

            assertThat(multiset).isEmpty();
        }

        @Test
        void multisetIteratorRemoveSingleOccurrence() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 1);

            var iterator = multiset.iterator();
            iterator.next();
            iterator.remove();

            assertThat(multiset).isEmpty();
        }

        @Test
        void multisetIteratorRemoveFromMultipleOccurrences() {
            var multiset = Multiset.newEnumMultiset(Color.class);
            multiset.add(Color.RED, 3);

            var iterator = multiset.iterator();
            iterator.next();
            iterator.remove();

            assertThat(multiset.count(Color.RED)).isEqualTo(2);
        }

        @Test
        void isActuallyEWithWrongEnumType() {
            var multiset = Multiset.newEnumMultiset(Color.class);

            // This uses a different enum type
            enum OtherEnum {
                A
            }
            assertThat(multiset.count(OtherEnum.A)).isZero();
        }
    }

    @Nested
    class HashMultisetTests {

        @Test
        void createEmpty() {
            var multiset = Multiset.<String>newHashMultiset();

            assertThat(multiset).isEmpty();
        }

        @Test
        void addAndCount() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 3);

            assertThat(multiset.count("apple")).isEqualTo(3);
            assertThat(multiset.size()).isEqualTo(3);
        }

        @Test
        void removeOccurrences() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 5);

            multiset.remove("apple", 2);

            assertThat(multiset.count("apple")).isEqualTo(3);
        }

        @Test
        void setCount() {
            var multiset = Multiset.<String>newHashMultiset();

            multiset.setCount("apple", 7);

            assertThat(multiset.count("apple")).isEqualTo(7);
        }

        @Test
        void elementSet() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 2);
            multiset.add("banana", 1);

            assertThat(multiset.elementSet()).containsExactlyInAnyOrder("apple", "banana");
        }

        @Test
        void iterator() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 2);

            var count = 0;
            for (var s : multiset) {
                assertThat(s).isEqualTo("apple");
                count++;
            }
            assertThat(count).isEqualTo(2);
        }

        @Test
        void iteratorRemove() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 3);

            var iterator = multiset.iterator();
            iterator.next();
            iterator.remove();

            assertThat(multiset.count("apple")).isEqualTo(2);
        }

        @Test
        void clear() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 2);
            multiset.add("banana", 1);

            multiset.clear();

            assertThat(multiset).isEmpty();
        }

        @Test
        void entryIteratorRemove() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 2);
            multiset.add("banana", 1);

            var iterator = multiset.entrySet().iterator();
            iterator.next();
            iterator.remove();

            assertThat(multiset.entrySet()).hasSize(1);
        }

        @Test
        void entryIteratorRemoveWithoutNext() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 1);

            var iterator = multiset.entrySet().iterator();

            assertThatThrownBy(iterator::remove).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void entryIteratorNextOnEmpty() {
            var multiset = Multiset.<String>newHashMultiset();
            var iterator = multiset.entrySet().iterator();

            assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        void addTooManyOccurrencesThrows() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", Integer.MAX_VALUE);

            assertThatThrownBy(() -> multiset.add("apple", 1)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void addZeroOccurrences() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 2);

            var oldCount = multiset.add("apple", 0);

            assertThat(oldCount).isEqualTo(2);
            assertThat(multiset.count("apple")).isEqualTo(2);
        }

        @Test
        void removeZeroOccurrences() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 2);

            var oldCount = multiset.remove("apple", 0);

            assertThat(oldCount).isEqualTo(2);
        }

        @Test
        void removeMoreThanExists() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 2);

            var oldCount = multiset.remove("apple", 5);

            assertThat(oldCount).isEqualTo(2);
            assertThat(multiset.count("apple")).isZero();
        }

        @Test
        void removeFromNonExistent() {
            var multiset = Multiset.<String>newHashMultiset();

            var oldCount = multiset.remove("apple", 1);

            assertThat(oldCount).isZero();
        }

        @Test
        void setCountToZero() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 3);

            var oldCount = multiset.setCount("apple", 0);

            assertThat(oldCount).isEqualTo(3);
            assertThat(multiset.count("apple")).isZero();
        }

        @Test
        void setCountOnExisting() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 3);

            var oldCount = multiset.setCount("apple", 7);

            assertThat(oldCount).isEqualTo(3);
            assertThat(multiset.count("apple")).isEqualTo(7);
        }

        @Test
        void setCountOnNonExistent() {
            var multiset = Multiset.<String>newHashMultiset();

            var oldCount = multiset.setCount("apple", 5);

            assertThat(oldCount).isZero();
            assertThat(multiset.count("apple")).isEqualTo(5);
        }

        @Test
        void iteratorRemoveLastOccurrence() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 1);

            var iterator = multiset.iterator();
            iterator.next();
            iterator.remove();

            assertThat(multiset).isEmpty();
        }

        @Test
        void iteratorRemoveWithoutNextThrows() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 1);

            var iterator = multiset.iterator();

            assertThatThrownBy(iterator::remove).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void elementSetIteratorRemove() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 3);

            var iterator = multiset.elementSet().iterator();
            iterator.next();
            iterator.remove();

            assertThat(multiset).isEmpty();
        }

        @Test
        void elementSetIteratorRemoveWithoutNextThrows() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 1);

            var iterator = multiset.elementSet().iterator();

            assertThatThrownBy(iterator::remove).isInstanceOf(IllegalStateException.class);
        }

        @Test
        void forEachEntry() {
            var multiset = Multiset.<String>newHashMultiset();
            multiset.add("apple", 2);
            multiset.add("banana", 3);

            var visited = new ArrayList<String>();
            multiset.forEachEntry((elem, count) -> visited.add(elem + ":" + count));

            assertThat(visited).containsExactlyInAnyOrder("apple:2", "banana:3");
        }

        @Test
        void equalsAndHashCode() {
            var multiset1 = Multiset.<String>newHashMultiset();
            multiset1.add("apple", 2);
            var multiset2 = Multiset.<String>newHashMultiset();
            multiset2.add("apple", 2);

            assertThat(multiset1).isEqualTo(multiset2);
            assertThat(multiset1.hashCode()).isEqualTo(multiset2.hashCode());
        }
    }

    @Nested
    class ImmutableMultisetTests {

        @Test
        void copyOfEmptyCollection() {
            var source = List.<String>of();

            var multiset = Multiset.copyOf(source);

            assertThat(multiset).isEmpty();
        }

        @Test
        void copyOfList() {
            var source = List.of("a", "b", "a", "c", "a");

            var multiset = Multiset.copyOf(source);

            assertThat(multiset.count("a")).isEqualTo(3);
            assertThat(multiset.count("b")).isEqualTo(1);
            assertThat(multiset.count("c")).isEqualTo(1);
        }

        @Test
        void copyOfMultiset() {
            var source = Multiset.<String>newHashMultiset();
            source.add("a", 5);
            source.add("b", 3);

            var multiset = Multiset.copyOf(source);

            assertThat(multiset.count("a")).isEqualTo(5);
            assertThat(multiset.count("b")).isEqualTo(3);
        }

        @Test
        void copyOfImmutableReturnsItself() {
            var source = Multiset.copyOf(List.of("a", "b"));

            var copy = Multiset.copyOf(source);

            assertThat(copy).isSameAs(source);
        }

        @Test
        void immutableThrowsOnAdd() {
            var multiset = Multiset.copyOf(List.of("a"));

            assertThatThrownBy(() -> multiset.add("b", 1)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void immutableThrowsOnRemove() {
            var multiset = Multiset.copyOf(List.of("a", "a"));

            assertThatThrownBy(() -> multiset.remove("a", 1)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void immutableThrowsOnSetCount() {
            var multiset = Multiset.copyOf(List.of("a"));

            assertThatThrownBy(() -> multiset.setCount("a", 5)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void immutableThrowsOnClear() {
            var multiset = Multiset.copyOf(List.of("a"));

            assertThatThrownBy(multiset::clear).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void elementSet() {
            var multiset = Multiset.copyOf(List.of("a", "b", "a"));

            assertThat(multiset.elementSet()).containsExactlyInAnyOrder("a", "b");
        }

        @Test
        void entrySet() {
            var multiset = Multiset.copyOf(List.of("a", "a", "b"));

            var entries = multiset.entrySet();
            assertThat(entries).hasSize(2);
        }

        @Test
        void iterator() {
            var multiset = Multiset.copyOf(List.of("a", "a", "b"));

            var elements = new ArrayList<String>();
            for (var s : multiset) {
                elements.add(s);
            }

            assertThat(elements).hasSize(3);
            assertThat(elements).contains("a", "a", "b");
        }

        @Test
        void iteratorRemoveThrows() {
            var multiset = Multiset.copyOf(List.of("a"));

            var iterator = multiset.iterator();
            iterator.next();

            assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void entrySetIteratorRemoveThrows() {
            var multiset = Multiset.copyOf(List.of("a"));

            var iterator = multiset.entrySet().iterator();
            iterator.next();

            assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void elementSetIteratorRemoveThrows() {
            var multiset = Multiset.copyOf(List.of("a"));

            var iterator = multiset.elementSet().iterator();
            iterator.next();

            assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void contains() {
            var multiset = Multiset.copyOf(List.of("a", "b"));

            assertThat(multiset.contains("a")).isTrue();
            assertThat(multiset.contains("c")).isFalse();
        }

        @Test
        void equalsAndHashCode() {
            var multiset1 = Multiset.copyOf(List.of("a", "a", "b"));
            var multiset2 = Multiset.copyOf(List.of("a", "b", "a"));

            assertThat(multiset1).isEqualTo(multiset2);
            assertThat(multiset1.hashCode()).isEqualTo(multiset2.hashCode());
        }

        @Test
        void toStringFormat() {
            var multiset = Multiset.copyOf(List.of("a", "a"));

            assertThat(multiset.toString()).contains("a");
        }

        @Test
        void entryEqualsAndHashCode() {
            var multiset = Multiset.copyOf(List.of("a", "a"));

            var entry = multiset.entrySet().iterator().next();

            assertThat(entry.element()).isEqualTo("a");
            assertThat(entry.count()).isEqualTo(2);
            assertThat(entry.toString()).contains("a");
        }

        @Test
        void forEachEntry() {
            var multiset = Multiset.copyOf(List.of("a", "a", "b"));

            var visited = new ArrayList<String>();
            multiset.forEachEntry((elem, count) -> visited.add(elem + ":" + count));

            assertThat(visited).containsExactlyInAnyOrder("a:2", "b:1");
        }
    }
}
