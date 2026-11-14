package com.redwood.collections.java;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UserJavaListTest {

	// ============================================================
	// Вспомогательные методы
	// ============================================================

	static UserJavaList<Integer> fromList(List<Integer> input) {
		UserJavaList<Integer> list = new UserJavaList<>();
		for (Integer v : input) list.add(v);
		return list;
	}

	static List<Integer> toList(UserJavaList<Integer> list) {
		List<Integer> out = new ArrayList<>(list.size());
		for (Integer v : list) out.add(v);
		return out;
	}

	static Object field(Object obj, String name) throws Exception {
		Class<?> c = obj.getClass();
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f.get(obj);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	static int countNodes(UserJavaList<?> list) throws Exception {
		Object head = field(list, "head");
		if (head == null) return 0;
		int n = 0;
		Object cur = head;
		while (cur != null) {
			n++;
			cur = field(cur, "next");
		}
		return n;
	}

	// ============================================================
	// Функциональные тесты: обязательный набор ЛР4
	// ============================================================

	static Stream<List<Integer>> functionalCases() {
		return Stream.of(
			List.of(),                          // пустой
			List.of(42),                        // один элемент
			List.of(1, 2, 3, 4, 5),             // прямой порядок
			List.of(5, 4, 3, 2, 1),             // обратный порядок
			List.of(3, 1, 4, 5, 2),             // неупорядочен
			List.of(7, 7, 7, 7, 7),             // одинаковые значения
			List.of(3, 1, 3, 2, 3),             // повторяющиеся
			List.of(1, 1, 3, 3, 2, 2, 5, 5),    // группы повторов
			List.of(5, 2, 9, 1, 3),             // макс. в середине
			List.of(9, 1, 3, 2, 4),             // макс. в начале
			List.of(1, 3, 2, 4, 9),             // макс. в конце
			List.of(9, 1, 9, 2, 9, 3)           // несколько макс.
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("functionalCases")
	void sortMatchesReference(List<Integer> input) {
		UserJavaList<Integer> list = fromList(input);
		list.sort(Integer::compare);
		List<Integer> expected = new ArrayList<>(input);
		Collections.sort(expected);
		assertEquals(expected, list);
	}

	@Test
	void emptyAndSingleElementDoNotCallComparator() {
		Comparator<Integer> exploding = (a, b) -> {
			throw new AssertionError("Comparator must not be called for size <= 1");
		};
		UserJavaList<Integer> empty = new UserJavaList<>();
		empty.sort(exploding);
		UserJavaList<Integer> single = new UserJavaList<>();
		single.add(1);
		single.sort(exploding);
	}

	@Test
	void nullComparatorRejectedForNonEmptyList() {
		UserJavaList<Integer> list = new UserJavaList<>();
		list.add(2);
		list.add(1);
		assertThrows(NullPointerException.class, () -> list.sort(null));
	}

	@Test
	void mergeSortIsStableForEqualKeys() {
		record Keyed(int key, String id) {}
		UserJavaList<Keyed> list = new UserJavaList<>();
		list.add(new Keyed(2, "a"));
		list.add(new Keyed(1, "b"));
		list.add(new Keyed(2, "c"));
		list.sort(Comparator.comparingInt(Keyed::key));
		assertEquals("b", list.get(0).id());
		assertEquals("a", list.get(1).id());
		assertEquals("c", list.get(2).id());
	}

	@Test
	void randomDataAcrossSizes() {
		Random rnd = new Random(42);
		for (int n : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 17, 32, 33}) {
			UserJavaList<Integer> list = new UserJavaList<>();
			List<Integer> data = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				int v = rnd.nextInt(21) - 10;
				data.add(v);
				list.add(v);
			}
			list.sort(Integer::compare);
			Collections.sort(data);
			assertEquals(data, list, "Size: " + n);
		}
	}

	// ============================================================
	// Структурное покрытие: ветви merge sort
	// ============================================================

	@Nested
	class StructuralCoverage {

		@Test
		void baseCaseEmpty() {
			// sortRec(node == null) -> Range(null, null)
			UserJavaList<Integer> list = new UserJavaList<>();
			list.sort(Integer::compare);
			assertEquals(0, list.size());
		}

		@Test
		void baseCaseSingle() {
			// sortRec(node.next == null) -> Range(node, node)
			UserJavaList<Integer> list = new UserJavaList<>();
			list.add(7);
			list.sort(Integer::compare);
			assertEquals(List.of(7), toList(list));
		}

		@Test
		void evenSizeSplit() {
			// fast/slow: чётное N
			UserJavaList<Integer> list = fromList(List.of(4, 3, 2, 1));
			list.sort(Integer::compare);
			assertEquals(List.of(1, 2, 3, 4), toList(list));
		}

		@Test
		void oddSizeSplit() {
			// fast/slow: нечётное N
			UserJavaList<Integer> list = fromList(List.of(5, 4, 3, 2, 1));
			list.sort(Integer::compare);
			assertEquals(List.of(1, 2, 3, 4, 5), toList(list));
		}

		@Test
		void mergeBranchLeftLess() {
			// cmp(left, right) < 0
			UserJavaList<Integer> list = fromList(List.of(1, 2, 3, 4));
			list.sort(Integer::compare);
			assertEquals(List.of(1, 2, 3, 4), toList(list));
		}

		@Test
		void mergeBranchEqual() {
			// cmp(left, right) == 0 — берётся левый
			UserJavaList<Integer> list = fromList(List.of(2, 1, 2, 1));
			list.sort(Integer::compare);
			assertEquals(List.of(1, 1, 2, 2), toList(list));
		}

		@Test
		void mergeBranchRightLess() {
			// cmp(left, right) > 0
			UserJavaList<Integer> list = fromList(List.of(3, 4, 1, 2));
			list.sort(Integer::compare);
			assertEquals(List.of(1, 2, 3, 4), toList(list));
		}

		@Test
		void mergeLeftExhaustedFirst() {
			// left == null -> rest = right
			UserJavaList<Integer> list = fromList(List.of(1, 2, 3, 4));
			list.sort(Integer::compare);
			assertEquals(List.of(1, 2, 3, 4), toList(list));
		}

		@Test
		void mergeRightExhaustedFirst() {
			// right == null -> rest = left
			UserJavaList<Integer> list = fromList(List.of(3, 4, 1, 2));
			list.sort(Integer::compare);
			assertEquals(List.of(1, 2, 3, 4), toList(list));
		}

		@Test
		void comparisonCountWithinNLogNBounds() {
			// Число сравнений ~ N log2 N
			for (int n : new int[]{64, 128, 256, 512, 1024}) {
				AtomicLong cmpCount = new AtomicLong();
				Comparator<Integer> counting = (a, b) -> {
					cmpCount.incrementAndGet();
					return Integer.compare(a, b);
				};
				UserJavaList<Integer> list = new UserJavaList<>();
				Random rnd = new Random(n);
				for (int i = 0; i < n; i++) list.add(rnd.nextInt());
				list.sort(counting);

				long log2n = (long) (Math.log(n) / Math.log(2));
				long lower = Math.max(1, (n * log2n) / 3);
				long upper = n * (log2n + 2);
				assertTrue(cmpCount.get() >= lower,
					"Too few comparisons: " + cmpCount.get() + " for n=" + n + " (>= " + lower + ")");
				assertTrue(cmpCount.get() <= upper,
					"Too many comparisons: " + cmpCount.get() + " for n=" + n + " (<= " + upper + ")");
			}
		}
	}

	// ============================================================
	// Полупрозрачный ящик: инварианты структуры
	// ============================================================

	@Nested
	class WhiteBoxInvariants {

		@Test
		void headPrevIsNullAfterSort() throws Exception {
			UserJavaList<Integer> list = fromList(List.of(3, 1, 4, 1, 5));
			list.sort(Integer::compare);
			Object head = field(list, "head");
			assertNotNull(head);
			assertNull(field(head, "prev"), "head.prev must be null after sort");
		}

		@Test
		void tailNextIsNullAfterSort() throws Exception {
			UserJavaList<Integer> list = fromList(List.of(3, 1, 4, 1, 5));
			list.sort(Integer::compare);
			Object tail = field(list, "tail");
			assertNotNull(tail);
			assertNull(field(tail, "next"), "tail.next must be null after sort");
		}

		@Test
		void sizePreservedAfterSort() {
			List<Integer> data = List.of(5, 1, 4, 2, 3);
			UserJavaList<Integer> list = fromList(data);
			list.sort(Integer::compare);
			assertEquals(data.size(), list.size());
		}

		@Test
		void nodeCountEqualsSizeAfterSort() throws Exception {
			for (int n : new int[]{0, 1, 2, 5, 17, 100}) {
				UserJavaList<Integer> list = new UserJavaList<>();
				Random rnd = new Random(n);
				for (int i = 0; i < n; i++) list.add(rnd.nextInt());
				list.sort(Integer::compare);
				assertEquals(n, countNodes(list), "nodeCount == size for n=" + n);
			}
		}

		@Test
		void bidirectionalTraversalConsistent() {
			List<Integer> data = List.of(5, 1, 4, 1, 3, 5, 2);
			UserJavaList<Integer> list = fromList(data);
			list.sort(Integer::compare);

			List<Integer> forward = new ArrayList<>(list);
			List<Integer> backward = new ArrayList<>(list.size());
			ListIterator<Integer> it = list.listIterator(list.size());
			while (it.hasPrevious()) backward.add(it.previous());
			Collections.reverse(backward);

			assertEquals(forward, backward, "Forward and backward traversal must agree");
		}

		@Test
		void iteratorCoversAllElementsInOrder() {
			List<Integer> data = List.of(3, 1, 4, 1, 5, 9, 2, 6);
			UserJavaList<Integer> list = fromList(data);
			list.sort(Integer::compare);

			List<Integer> viaIterator = new ArrayList<>();
			for (Integer v : list) viaIterator.add(v);

			List<Integer> expected = new ArrayList<>(data);
			Collections.sort(expected);
			assertEquals(expected, viaIterator);
		}

		@Test
		void elementsPreservedAsMultiset() {
			List<Integer> data = List.of(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5);
			UserJavaList<Integer> list = fromList(data);
			list.sort(Integer::compare);

			List<Integer> sorted = new ArrayList<>(data);
			Collections.sort(sorted);
			for (int i = 0; i < sorted.size(); i++) {
				assertEquals(sorted.get(i), list.get(i), "get(" + i + ")");
			}
		}
	}

	// ============================================================
	// Обнаружение дефектов (документированные тесты)
	// ============================================================

	@Nested
	class DefectDetection {

		/**
		 * Дефект: в merge не обнуляется newHead.prev.
		 * Ловится white-box тестом на head.prev.
		 */
		@Test
		void detectsDefectHeadPrevNotReset() throws Exception {
			UserJavaList<Integer> list = fromList(List.of(4, 3, 2, 1));
			list.sort(Integer::compare);
			assertNull(field(field(list, "head"), "prev"));
		}

		/**
		 * Дефект: в merge не обнуляется tail.next (актуально, если split
		 * не отрезал хвост). Ловится white-box тестом на tail.next.
		 */
		@Test
		void detectsDefectTailNextNotReset() throws Exception {
			UserJavaList<Integer> list = fromList(List.of(4, 3, 2, 1));
			list.sort(Integer::compare);
			assertNull(field(field(list, "tail"), "next"));
		}

		/**
		 * Дефект: использование '<' вместо '<=' в merge нарушает устойчивость.
		 */
		@Test
		void detectsDefectUnstableMerge() {
			record Keyed(int key, String id) {}
			UserJavaList<Keyed> list = new UserJavaList<>();
			list.add(new Keyed(1, "a"));
			list.add(new Keyed(1, "b"));
			list.add(new Keyed(1, "c"));
			list.sort(Comparator.comparingInt(Keyed::key));
			assertEquals("a", list.get(0).id());
			assertEquals("b", list.get(1).id());
			assertEquals("c", list.get(2).id());
		}

		/**
		 * Дефект: убрана проверка null-компаратора.
		 * Ловится тем, что для size>1 должно быть NPE (до merge, а не внутри).
		 */
		@Test
		void detectsDefectNoNullComparatorCheck() {
			UserJavaList<Integer> list = fromList(List.of(1, 2));
			assertThrows(NullPointerException.class, () -> list.sort(null));
		}

		/**
		 * Дефект: потеря/дублирование элемента при split/merge.
		 * Ловится сравнением содержимого с эталоном.
		 */
		@Test
		void detectsDefectSizeOrContentCorruption() {
			List<Integer> data = List.of(2, 1, 4, 3, 6, 5, 8, 7);
			UserJavaList<Integer> list = fromList(data);
			list.sort(Integer::compare);
			List<Integer> expected = new ArrayList<>(data);
			Collections.sort(expected);
			assertEquals(expected, toList(list));
			assertEquals(data.size(), list.size());
		}
	}

	// ============================================================
	// Производительность и память
	// ============================================================

	// ============================================================
	// Производительность и память
	// ============================================================

	// ============================================================
	// Производительность и память
	// ============================================================

	// ============================================================
	// Производительность и память
	// ============================================================

	@Nested
	class PerformanceAndMemory {

		@Test
		void printFullPerformanceTable() {
			int[] sizes = {1000, 2000, 4000, 8000, 16000};

			System.out.println("\n=== Java: Замер производительности и памяти ===");
			System.out.printf("%-12s\t%-12s\t%-12s\t%-12s%n", "N", "Comparisons", "Mem (KB)", "Time (ms)");

			for (int n : sizes) {
				System.gc();
				Runtime rt = Runtime.getRuntime();
				long memBefore = rt.totalMemory() - rt.freeMemory();

				UserJavaList<Integer> list = new UserJavaList<>();
				Random rnd = new Random(42);
				for (int i = 0; i < n; i++) list.add(rnd.nextInt());

				AtomicLong cmp = new AtomicLong();
				Comparator<Integer> countingCmp = (a, b) -> {
					cmp.incrementAndGet();
					return Integer.compare(a, b);
				};

				long t0 = System.nanoTime();
				list.sort(countingCmp);
				double timeMs = (System.nanoTime() - t0) / 1_000_000.0;

				long memAfter = rt.totalMemory() - rt.freeMemory();
				long memKb = (memAfter - memBefore) / 1024;

				// %-12d для целых чисел, %-12.3f для дробных
				System.out.printf("%-12d\t%-12d\t%-12d\t%-12.3f%n", n, cmp.get(), memKb, timeMs);

				double upperBound = n * (Math.log(n) / Math.log(2) + 2);
				assertTrue(cmp.get() <= upperBound, "Too many comparisons: " + cmp.get());
			}
		}
	}
}
