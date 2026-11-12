package com.redwood.collections.kotlin

import com.redwood.collections_api.IUserList
import tools.jackson.databind.SerializationFeature
import tools.jackson.dataformat.xml.XmlMapper
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.util.*

/**
 * Двусвязный разомкнутый список на Kotlin.
 * Реализует [IUserList]; содержит две реализации merge sort — функциональную (top-down)
 * и императивную (bottom-up / циклическое слияние) — и Kotlin-идиоматичные [forEach] и [firstThat].
 */
class UserKotlinList<T> : IUserList<T> {

    private class Node<T>(var data: T) {
        var prev: Node<T>? = null
        var next: Node<T>? = null
    }

    private var head: Node<T>? = null
    private var tail: Node<T>? = null
    private var count: Int = 0
    private var modCount: Int = 0

    override fun size(): Int = count

    private fun getNode(index: Int): Node<T> {
        require(index in 0 until count) { "Index: $index, Size: $count" }
        return if (index <= count / 2) {
            var cur = head!!
            repeat(index) { cur = cur.next!! }
            cur
        } else {
            var cur = tail!!
            for (i in count - 1 downTo index + 1) cur = cur.prev!!
            cur
        }
    }

    override operator fun get(index: Int): T = getNode(index).data

    override operator fun set(index: Int, element: T): T {
        val node = getNode(index)
        val old = node.data
        node.data = element
        return old
    }

    override fun add(element: T): Boolean {
        add(count, element)
        return true
    }

    override fun add(index: Int, element: T) {
        require(index in 0..count) { "Index: $index, Size: $count" }
        val node = Node(element)
        when (index) {
            0 -> if (head == null) {
                head = node
                tail = node
            } else {
                node.next = head
                head!!.prev = node
                head = node
            }
            count -> {
                node.prev = tail
                tail!!.next = node
                tail = node
            }
            else -> {
                val cur = getNode(index)
                val prv = cur.prev!!
                node.prev = prv
                node.next = cur
                prv.next = node
                cur.prev = node
            }
        }
        count++
        modCount++
    }

    private fun unlink(node: Node<T>): T {
        val data = node.data
        val p = node.prev
        val n = node.next
        if (p != null) p.next = n else head = n
        if (n != null) n.prev = p else tail = p
        node.prev = null
        node.next = null
        count--
        modCount++
        return data
    }

    override fun remove(index: Int): T = unlink(getNode(index))

    override fun remove(o: Any?): Boolean {
        var cur = head
        while (cur != null) {
            if (cur.data == o) {
                unlink(cur)
                return true
            }
            cur = cur.next
        }
        return false
    }

    override fun clear() {
        head = null
        tail = null
        count = 0
        modCount++
    }

    /** Эффективный обход без создания итератора; затеняет расширение [Iterable.forEach]. */
    fun forEach(action: (T) -> Unit) {
        var cur = head
        while (cur != null) {
            action(cur.data)
            cur = cur.next
        }
    }

    /** Первый элемент, удовлетворяющий предикату, либо `null`. */
    fun firstThat(predicate: (T) -> Boolean): T? {
        var cur = head
        while (cur != null) {
            if (predicate(cur.data)) return cur.data
            cur = cur.next
        }
        return null
    }

    override operator fun iterator(): MutableIterator<T> = listIterator()

    override fun listIterator(): MutableListIterator<T> = object : MutableListIterator<T> {
        private var nextNode: Node<T>? = head
        private var lastReturned: Node<T>? = null
        private var nextIdx: Int = 0
        private var expectedModCount: Int = modCount

        private fun checkMod() {
            if (modCount != expectedModCount)
                throw ConcurrentModificationException("Concurrent modification")
        }

        override fun hasNext(): Boolean = nextIdx < count

        override fun next(): T {
            checkMod()
            if (!hasNext()) throw NoSuchElementException()
            val current = nextNode!!
            lastReturned = current
            nextNode = current.next
            nextIdx++
            return current.data
        }

        override fun hasPrevious(): Boolean = nextIdx > 0

        override fun previous(): T {
            checkMod()
            if (!hasPrevious()) throw NoSuchElementException()
            val current = nextNode?.prev ?: tail!!
            nextNode = current
            lastReturned = current
            nextIdx--
            return current.data
        }

        override fun nextIndex(): Int = nextIdx
        override fun previousIndex(): Int = nextIdx - 1

        override fun remove() {
            checkMod()
            val removed = lastReturned ?: throw IllegalStateException()
            val lastNext = removed.next
            unlink(removed)
            if (nextNode === removed) nextNode = lastNext else nextIdx--
            lastReturned = null
            expectedModCount = modCount
        }

        override fun set(element: T) {
            checkMod()
            val node = lastReturned ?: throw IllegalStateException()
            node.data = element
        }

        override fun add(element: T) {
            checkMod()
            val newNode = Node(element)
            val nx = nextNode
            if (nx != null) {
                newNode.prev = nx.prev
                newNode.next = nx
                if (nx.prev == null) head = newNode else nx.prev!!.next = newNode
                nx.prev = newNode
            } else {
                newNode.prev = tail
                if (tail == null) head = newNode else tail!!.next = newNode
                tail = newNode
            }
            nextIdx++
            count++
            modCount++
            lastReturned = null
            expectedModCount = modCount
        }
    }

    /** Мост для Java-интерфейса; лямбда с двумя параметрами конвертируется в [Comparator] (SAM). */
    override fun sort(c: Comparator<in T>?) {
        requireNotNull(c) { "Comparator cannot be null" }
        sortImperative { a, b -> c.compare(a, b) }
    }

    /** Функциональная сортировка: рекурсивное разделение + рекурсивное слияние. */
    fun sortFunctional(cmp: (T, T) -> Int) {
        if (count <= 1) return
        val (h, t) = mergeSortRec(head, cmp)
        head = h
        tail = t
        modCount++
    }

    /** Императивная сортировка: bottom-up merge sort (циклическое слияние), O(N log N) по времени, O(1) по памяти. */
    fun sortImperative(cmp: (T, T) -> Int) {
        if (count <= 1) return
        var step = 1
        while (step < count) {
            var cur = head
            var newHead: Node<T>? = null
            var newTail: Node<T>? = null
            while (cur != null) {
                val (left, rest1) = splitRun(cur, step)
                val (right, rest2) = splitRun(rest1, step)
                val (mh, mt) = mergeTwo(left, right, cmp)
                if (newHead == null) {
                    newHead = mh
                    newTail = mt
                } else {
                    newTail!!.next = mh
                    mh!!.prev = newTail
                    newTail = mt
                }
                cur = rest2
            }
            head = newHead
            tail = newTail
            step *= 2
        }
        modCount++
    }

    private fun mergeSortRec(node: Node<T>?, cmp: (T, T) -> Int): Pair<Node<T>?, Node<T>?> {
        if (node == null) return null to null
        if (node.next == null) return node to node
        var fast: Node<T>? = node
        var slow: Node<T>? = node
        while (fast?.next?.next != null) {
            slow = slow!!.next
            fast = fast.next!!.next
        }
        val half = slow!!.next
        slow.next = null
        half!!.prev = null
        val (lh, _) = mergeSortRec(node, cmp)
        val (rh, _) = mergeSortRec(half, cmp)
        return mergeTwo(lh, rh, cmp)
    }

    private fun splitRun(start: Node<T>?, n: Int): Pair<Node<T>?, Node<T>?> {
        if (start == null) return null to null
        var cur = start
        var i = 1
        while (i < n && cur?.next != null) {
            cur = cur.next!!
            i++
        }
        val rest = cur.next
        cur.next = null
        rest?.prev = null
        return start to rest
    }

    private fun mergeTwo(a: Node<T>?, b: Node<T>?, cmp: (T, T) -> Int): Pair<Node<T>?, Node<T>?> {
        if (a == null) return b to b?.let(::tailOf)
        if (b == null) return a to tailOf(a)
        var l: Node<T>? = a
        var r: Node<T>? = b
        val resultHead: Node<T>
        var resultTail: Node<T>
        if (cmp(a.data, b.data) <= 0) {
            resultHead = a
            resultTail = a
            l = a.next
        } else {
            resultHead = b
            resultTail = b
            r = b.next
        }
        resultHead.prev = null
        while (true) {
            val ln = l ?: break
            val rn = r ?: break
            val pick: Node<T>
            if (cmp(ln.data, rn.data) <= 0) {
                pick = ln
                l = ln.next
            } else {
                pick = rn
                r = rn.next
            }
            resultTail.next = pick
            pick.prev = resultTail
            resultTail = pick
        }
        val rest = l ?: r
        if (rest != null) {
            resultTail.next = rest
            rest.prev = resultTail
            resultTail = tailOf(rest)
        }
        return resultHead to resultTail
    }

    private fun tailOf(start: Node<T>): Node<T> {
        var cur = start
        while (cur.next != null) cur = cur.next!!
        return cur
    }

    @Throws(IOException::class)
    override fun save(out: Writer) {
        XmlMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build()
            .writeValue(out, this)
    }

    @Throws(IOException::class)
    override fun load(input: Reader, elementType: Class<out T>) {
        val mapper = XmlMapper.builder().build()
        clear()
        mapper.readerFor(elementType).readValues<T>(input).use { iter ->
            while (iter.hasNext()) add(iter.next())
        }
    }
}
