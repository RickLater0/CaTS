package com.redwood.collections.kotlin

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.lang.reflect.Field
import java.util.Random
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Stream

class UserKotlinListTest {

    companion object {
        @JvmStatic
        fun functionalCases(): Stream<List<Int>> = Stream.of(
            listOf(), listOf(42), listOf(1, 2, 3, 4, 5), listOf(5, 4, 3, 2, 1),
            listOf(3, 1, 4, 5, 2), listOf(7, 7, 7, 7, 7), listOf(3, 1, 3, 2, 3),
            listOf(1, 1, 3, 3, 2, 2, 5, 5), listOf(5, 2, 9, 1, 3), listOf(9, 1, 3, 2, 4),
            listOf(1, 3, 2, 4, 9), listOf(9, 1, 9, 2, 9, 3)
        )

        @JvmStatic
        fun getField(obj: Any, name: String): Any? {
            var c: Class<*>? = obj.javaClass
            while (c != null) {
                try {
                    val f: Field = c.getDeclaredField(name)
                    f.isAccessible = true
                    return f.get(obj)
                } catch (_: NoSuchFieldException) {
                    c = c.superclass
                }
            }
            throw NoSuchFieldException(name)
        }

        @JvmStatic
        fun countNodes(list: UserKotlinList<*>): Int {
            val head = getField(list, "head") ?: return 0
            var n = 0
            var cur: Any? = head
            while (cur != null) {
                n++
                cur = getField(cur, "next")
            }
            return n
        }
    }

    private fun <T> UserKotlinList<T>.toList(): List<T> = (0 until size()).map { get(it) }

    // ============================================================
    // Функциональные тесты
    // ============================================================

    @ParameterizedTest(name = "{0}")
    @MethodSource("functionalCases")
    fun `sortImperative matches reference`(input: List<Int>) {
        val list = UserKotlinList<Int>()
        input.forEach { list.add(it) }
        list.sortImperative { a, b -> a.compareTo(b) }
        assertEquals(input.sorted(), list.toList())
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("functionalCases")
    fun `sortFunctional matches reference`(input: List<Int>) {
        val list = UserKotlinList<Int>()
        input.forEach { list.add(it) }
        list.sortFunctional { a, b -> a.compareTo(b) }
        assertEquals(input.sorted(), list.toList())
    }

    @Test
    fun `empty and single element do not call comparator`() {
        val exploding = { _: Int, _: Int -> throw AssertionError("Must not be called") }
        val empty = UserKotlinList<Int>()
        empty.sortImperative(exploding)
        empty.sortFunctional(exploding)
        val single = UserKotlinList<Int>()
        single.add(1)
        single.sortImperative(exploding)
        single.sortFunctional(exploding)
    }

    @Test
    fun `null comparator rejected for java bridge`() {
        val list = UserKotlinList<Int>()
        list.add(1)
        list.add(2)
        assertThrows(NullPointerException::class.java) { list.sort(null) }
    }

    @Test
    fun `merge sort is stable for equal keys (imperative)`() {
        data class Keyed(val key: Int, val id: String)
        val list = UserKotlinList<Keyed>()
        list.add(Keyed(2, "a"))
        list.add(Keyed(1, "b"))
        list.add(Keyed(2, "c"))
        list.sortImperative { a, b -> a.key.compareTo(b.key) }
        assertEquals("b", list[0].id)
        assertEquals("a", list[1].id)
        assertEquals("c", list[2].id)
    }

    @Test
    fun `merge sort is stable for equal keys (functional)`() {
        data class Keyed(val key: Int, val id: String)
        val list = UserKotlinList<Keyed>()
        list.add(Keyed(2, "a"))
        list.add(Keyed(1, "b"))
        list.add(Keyed(2, "c"))
        list.sortFunctional { a, b -> a.key.compareTo(b.key) }
        assertEquals("b", list[0].id)
        assertEquals("a", list[1].id)
        assertEquals("c", list[2].id)
    }

    @Test
    fun `random data across sizes (imperative)`() {
        val rnd = Random(42)
        for (n in intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 17, 32, 33)) {
            val list = UserKotlinList<Int>()
            val data = mutableListOf<Int>()
            repeat(n) {
                val v = rnd.nextInt(21) - 10
                data.add(v)
                list.add(v)
            }
            list.sortImperative { a, b -> a.compareTo(b) }
            assertEquals(data.sorted(), list.toList(), "Size: $n")
        }
    }

    @Test
    fun `random data across sizes (functional)`() {
        val rnd = Random(42)
        for (n in intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 17, 32, 33)) {
            val list = UserKotlinList<Int>()
            val data = mutableListOf<Int>()
            repeat(n) {
                val v = rnd.nextInt(21) - 10
                data.add(v)
                list.add(v)
            }
            list.sortFunctional { a, b -> a.compareTo(b) }
            assertEquals(data.sorted(), list.toList(), "Size: $n")
        }
    }

    // ============================================================
    // Структурное покрытие
    // ============================================================

    @Nested
    inner class StructuralCoverage {

        @Test
        fun `base case empty - both sorts`() {
            val a = UserKotlinList<Int>()
            a.sortImperative { x, y -> x.compareTo(y) }
            val b = UserKotlinList<Int>()
            b.sortFunctional { x, y -> x.compareTo(y) }
            assertEquals(0, a.size()); assertEquals(0, b.size())
        }

        @Test
        fun `base case single - both sorts`() {
            val a = UserKotlinList<Int>(); a.add(7)
            a.sortImperative { x, y -> x.compareTo(y) }
            val b = UserKotlinList<Int>(); b.add(7)
            b.sortFunctional { x, y -> x.compareTo(y) }
            assertEquals(listOf(7), a.toList()); assertEquals(listOf(7), b.toList())
        }

        @Test
        fun `even size split`() {
            val a = UserKotlinList<Int>(); listOf(4, 3, 2, 1).forEach(a::add)
            a.sortImperative { x, y -> x.compareTo(y) }
            assertEquals(listOf(1, 2, 3, 4), a.toList())
            val b = UserKotlinList<Int>(); listOf(4, 3, 2, 1).forEach(b::add)
            b.sortFunctional { x, y -> x.compareTo(y) }
            assertEquals(listOf(1, 2, 3, 4), b.toList())
        }

        @Test
        fun `odd size split`() {
            val a = UserKotlinList<Int>(); listOf(5, 4, 3, 2, 1).forEach(a::add)
            a.sortImperative { x, y -> x.compareTo(y) }
            assertEquals(listOf(1, 2, 3, 4, 5), a.toList())
            val b = UserKotlinList<Int>(); listOf(5, 4, 3, 2, 1).forEach(b::add)
            b.sortFunctional { x, y -> x.compareTo(y) }
            assertEquals(listOf(1, 2, 3, 4, 5), b.toList())
        }

        @Test
        fun `merge branches less, equal, greater`() {
            data class Pair2(val a: Int, val b: Int)
            // less
            run {
                val l = UserKotlinList<Int>(); listOf(1, 2, 3, 4).forEach(l::add)
                l.sortImperative { x, y -> x.compareTo(y) }
                assertEquals(listOf(1, 2, 3, 4), l.toList())
            }
            // equal
            run {
                val l = UserKotlinList<Int>(); listOf(2, 1, 2, 1).forEach(l::add)
                l.sortFunctional { x, y -> x.compareTo(y) }
                assertEquals(listOf(1, 1, 2, 2), l.toList())
            }
            // greater
            run {
                val l = UserKotlinList<Int>(); listOf(3, 4, 1, 2).forEach(l::add)
                l.sortImperative { x, y -> x.compareTo(y) }
                assertEquals(listOf(1, 2, 3, 4), l.toList())
            }
        }

        @Test
        fun `comparison count within N log N bounds (both sorts)`() {
            for (n in intArrayOf(64, 128, 256, 512, 1024)) {
                for (impl in 0..1) {
                    val cnt = AtomicLong()
                    val cmp = { a: Int, b: Int -> cnt.incrementAndGet(); a.compareTo(b) }
                    val list = UserKotlinList<Int>()
                    val rnd = Random(n.toLong())
                    repeat(n) { list.add(rnd.nextInt()) }
                    if (impl == 0) list.sortImperative(cmp) else list.sortFunctional(cmp)
                    val log2n = (Math.log(n.toDouble()) / Math.log(2.0)).toLong()
                    val lower = maxOf(1L, n.toLong() * log2n / 3)
                    val upper = n.toLong() * (log2n + 2)
                    assertTrue(cnt.get() in lower..upper,
                        "impl=$impl n=$n cmp=${cnt.get()} not in [$lower,$upper]")
                }
            }
        }
    }

    // ============================================================
    // Полупрозрачный ящик
    // ============================================================

    @Nested
    inner class WhiteBoxInvariants {

        @Test
        fun `head prev and tail next are null after sort (both sorts)`() {
            for (impl in 0..1) {
                val list = UserKotlinList<Int>()
                listOf(3, 1, 4, 1, 5).forEach(list::add)
                if (impl == 0) list.sortImperative { a, b -> a.compareTo(b) }
                else list.sortFunctional { a, b -> a.compareTo(b) }
                val head = getField(list, "head")
                val tail = getField(list, "tail")
                assertNotNull(head); assertNotNull(tail)
                assertNull(getField(head!!, "prev"), "impl=$impl head.prev must be null")
                assertNull(getField(tail!!, "next"), "impl=$impl tail.next must be null")
            }
        }

        @Test
        fun `size and node count preserved (both sorts)`() {
            for (impl in 0..1) {
                for (n in intArrayOf(0, 1, 2, 5, 17, 100)) {
                    val list = UserKotlinList<Int>()
                    val rnd = Random(n.toLong())
                    repeat(n) { list.add(rnd.nextInt()) }
                    if (impl == 0) list.sortImperative { a, b -> a.compareTo(b) }
                    else list.sortFunctional { a, b -> a.compareTo(b) }
                    assertEquals(n, list.size())
                    assertEquals(n, countNodes(list))
                }
            }
        }

        @Test
        fun `bidirectional traversal consistent (both sorts)`() {
            for (impl in 0..1) {
                val data = listOf(5, 1, 4, 1, 3, 5, 2)
                val list = UserKotlinList<Int>()
                data.forEach(list::add)
                if (impl == 0) list.sortImperative { a, b -> a.compareTo(b) }
                else list.sortFunctional { a, b -> a.compareTo(b) }
                val fwd = list.toList()
                val bwd = mutableListOf<Int>()
                val it = list.listIterator(list.size())
                while (it.hasPrevious()) bwd.add(it.previous())
                bwd.reverse()
                assertEquals(fwd, bwd, "impl=$impl")
            }
        }

        @Test
        fun `elements preserved as multiset (both sorts)`() {
            for (impl in 0..1) {
                val data = listOf(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5)
                val list = UserKotlinList<Int>()
                data.forEach(list::add)
                if (impl == 0) list.sortImperative { a, b -> a.compareTo(b) }
                else list.sortFunctional { a, b -> a.compareTo(b) }
                assertEquals(data.sorted(), list.toList(), "impl=$impl")
            }
        }
    }

    // ============================================================
    // Обнаружение дефектов
    // ============================================================

    @Nested
    inner class DefectDetection {

        @Test
        fun `detects defect head prev not reset`() {
            for (impl in 0..1) {
                val list = UserKotlinList<Int>()
                listOf(4, 3, 2, 1).forEach(list::add)
                if (impl == 0) list.sortImperative { a, b -> a.compareTo(b) }
                else list.sortFunctional { a, b -> a.compareTo(b) }
                assertNull(getField(getField(list, "head")!!, "prev"), "impl=$impl")
            }
        }

        @Test
        fun `detects defect tail next not reset`() {
            for (impl in 0..1) {
                val list = UserKotlinList<Int>()
                listOf(4, 3, 2, 1).forEach(list::add)
                if (impl == 0) list.sortImperative { a, b -> a.compareTo(b) }
                else list.sortFunctional { a, b -> a.compareTo(b) }
                assertNull(getField(getField(list, "tail")!!, "next"), "impl=$impl")
            }
        }

        @Test
        fun `detects defect unstable merge`() {
            data class Keyed(val key: Int, val id: String)
            for (impl in 0..1) {
                val list = UserKotlinList<Keyed>()
                list.add(Keyed(1, "a")); list.add(Keyed(1, "b")); list.add(Keyed(1, "c"))
                if (impl == 0) list.sortImperative { a, b -> a.key.compareTo(b.key) }
                else list.sortFunctional { a, b -> a.key.compareTo(b.key) }
                assertEquals("a", list[0].id, "impl=$impl")
                assertEquals("b", list[1].id, "impl=$impl")
                assertEquals("c", list[2].id, "impl=$impl")
            }
        }

        @Test
        fun `detects defect no null comparator check`() {
            val list = UserKotlinList<Int>()
            list.add(1); list.add(2)
            assertThrows(NullPointerException::class.java) { list.sort(null) }
        }

        @Test
        fun `detects defect size or content corruption`() {
            for (impl in 0..1) {
                val data = listOf(2, 1, 4, 3, 6, 5, 8, 7)
                val list = UserKotlinList<Int>()
                data.forEach(list::add)
                if (impl == 0) list.sortImperative { a, b -> a.compareTo(b) }
                else list.sortFunctional { a, b -> a.compareTo(b) }
                assertEquals(data.sorted(), list.toList(), "impl=$impl")
                assertEquals(data.size, list.size())
            }
        }
    }

    // ============================================================
    // Производительность и память
    // ============================================================

    @Nested
    inner class PerformanceAndMemory {

        @Test
        fun `print full performance table`() {
            val sizes = intArrayOf(1000, 2000, 4000, 8000, 16000)

            for (impl in 0..1) {
                val name = if (impl == 0) "Imperative" else "Functional"
                println("\n=== Kotlin ($name): Замер производительности и памяти ===")
                // Форматируем заголовок с выравниванием влево
                println("%-12s\t%-12s\t%-12s\t%-12s".format("N", "Comparisons", "Mem (KB)", "Time (ms)"))

                for (n in sizes) {
                    System.gc()
                    val rt = Runtime.getRuntime()
                    val memBefore = rt.totalMemory() - rt.freeMemory()

                    val list = UserKotlinList<Int>()
                    val rnd = Random(42)
                    repeat(n) { list.add(rnd.nextInt()) }

                    val swaps = AtomicLong()
                    val cmp = { a: Int, b: Int ->
                        swaps.incrementAndGet()
                        a.compareTo(b)
                    }

                    val t0 = System.nanoTime()
                    if (impl == 0) list.sortImperative(cmp) else list.sortFunctional(cmp)
                    val timeMs = (System.nanoTime() - t0) / 1_000_000.0

                    val memAfter = rt.totalMemory() - rt.freeMemory()
                    val memKb = (memAfter - memBefore) / 1024

                    // Вывод строки с выравниванием
                    println("%-12d\t%-12d\t%-12d\t%-12.3f".format(n, swaps.get(), memKb, timeMs))

                    val upperBound = n * (Math.log(n.toDouble()) / Math.log(2.0) + 2)
                    assertTrue(swaps.get() <= upperBound, "$name Too many comparisons: ${swaps.get()}")
                }
            }
        }
    }
}
