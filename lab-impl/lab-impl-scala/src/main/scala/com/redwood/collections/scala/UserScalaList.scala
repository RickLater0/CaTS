package com.redwood.collections.scala

import com.redwood.collections_api.IUserList
import tools.jackson.databind.SerializationFeature
import tools.jackson.dataformat.xml.XmlMapper

import java.io.{Reader, Writer}
import java.util
import java.util.{Comparator, ConcurrentModificationException, NoSuchElementException}

/**
 * Двусвязный разомкнутый список на Scala.
 */
class UserScalaList[T] extends IUserList[T] {

	private final class Node(var data: T) {
		var prev: Option[Node] = None
		var next: Option[Node] = None
	}

	private var head: Option[Node] = None
	private var tail: Option[Node] = None
	private var _size: Int = 0
	private var modCount: Int = 0

	override def size(): Int = _size

	private def getNode(index: Int): Node = {
		if (index < 0 || index >= _size)
			throw new IndexOutOfBoundsException(s"Index: $index, Size: $_size")
		if (index <= _size / 2) {
			var cur = head.get
			var i = 0
			while (i < index) { cur = cur.next.get; i += 1 }
			cur
		} else {
			var cur = tail.get
			var i = _size - 1
			while (i > index) { cur = cur.prev.get; i -= 1 }
			cur
		}
	}

	override def get(index: Int): T = getNode(index).data

	override def set(index: Int, elem: T): T = {
		val n = getNode(index)
		val old = n.data
		n.data = elem
		old
	}

	override def add(elem: T): Boolean = { add(_size, elem); true }

	override def add(index: Int, elem: T): Unit = {
		if (index < 0 || index > _size)
			throw new IndexOutOfBoundsException(s"Index: $index, Size: $_size")
		val node = new Node(elem)
		if (index == 0) {
			if (head.isEmpty) { head = Some(node); tail = Some(node) }
			else {
				node.next = head
				head.foreach(_.prev = Some(node))
				head = Some(node)
			}
		} else if (index == _size) {
			node.prev = tail
			tail.foreach(_.next = Some(node))
			tail = Some(node)
		} else {
			val cur = getNode(index)
			val prv = cur.prev.get
			node.prev = Some(prv); node.next = Some(cur)
			prv.next = Some(node); cur.prev = Some(node)
		}
		_size += 1
		modCount += 1
	}

	private def rm(node: Node): T = {
		val data = node.data
		node.prev match {
			case Some(p) => p.next = node.next
			case None    => head = node.next
		}
		node.next match {
			case Some(n) => n.prev = node.prev
			case None    => tail = node.prev
		}
		node.prev = None; node.next = None
		node.data = null.asInstanceOf[T]
		_size -= 1
		modCount += 1
		data
	}

	override def remove(index: Int): T = rm(getNode(index))

	override def remove(elem: Any): Boolean = {
		var cur = head
		while (cur.isDefined) {
			if (cur.get.data == elem) { rm(cur.get); return true }
			cur = cur.get.next
		}
		false
	}

	override def clear(): Unit = {
		head = None; tail = None; _size = 0; modCount += 1
	}

	override def iterator(): java.util.Iterator[T] = listIterator()

	override def listIterator(): util.ListIterator[T] = {
		new util.ListIterator[T] {
			private var nextNode: Option[Node] = head
			private var lastReturned: Option[Node] = None
			private var nextIdx: Int = 0
			private var expectedModCount: Int = modCount

			private def checkMod(): Unit =
				if (modCount != expectedModCount)
					throw new ConcurrentModificationException("Concurrent modification")

			override def hasNext: Boolean = nextIdx < _size

			override def next(): T = {
				checkMod()
				if (!hasNext) throw new NoSuchElementException()
				val current = nextNode.get
				lastReturned = nextNode; nextNode = current.next; nextIdx += 1
				current.data
			}

			override def hasPrevious: Boolean = nextIdx > 0

			override def previous(): T = {
				checkMod()
				if (!hasPrevious) throw new NoSuchElementException()
				val current = nextNode match {
					case Some(n) => n.prev.get
					case None    => tail.get
				}
				nextNode = Some(current); lastReturned = Some(current); nextIdx -= 1
				current.data
			}

			override def nextIndex(): Int = nextIdx
			override def previousIndex(): Int = nextIdx - 1

			override def remove(): Unit = {
				checkMod()
				if (lastReturned.isEmpty) throw new IllegalStateException()
				val removed = lastReturned.get
				val lastNext = removed.next
				UserScalaList.this.rm(removed)
				if (nextNode.contains(removed)) nextNode = lastNext else nextIdx -= 1
				lastReturned = None; expectedModCount = modCount
			}

			override def set(elem: T): Unit = {
				checkMod()
				if (lastReturned.isEmpty) throw new IllegalStateException()
				lastReturned.get.data = elem
			}

			override def add(elem: T): Unit = {
				checkMod()
				val newNode = new Node(elem)
				nextNode match {
					case Some(n) =>
						newNode.prev = n.prev; newNode.next = Some(n)
						n.prev match {
							case Some(p) => p.next = Some(newNode)
							case None    => head = Some(newNode)
						}
						n.prev = Some(newNode)
					case None =>
						newNode.prev = tail
						tail match {
							case Some(t) => t.next = Some(newNode)
							case None    => head = Some(newNode)
						}
						tail = Some(newNode)
				}
				nextIdx += 1; _size += 1; modCount += 1
				lastReturned = None; expectedModCount = modCount
			}
		}
	}

	override def sort(cmp: Comparator[? >: T]): Unit = {
		if (cmp == null) throw new NullPointerException("Comparator cannot be null")
		if (_size > 1) {
			val (h, t) = mergeSortRec(head, cmp)
			head = h; tail = t; modCount += 1
		}
	}

	private def mergeSortRec(node: Option[Node], cmp: Comparator[? >: T]): (Option[Node], Option[Node]) = {
		node match {
			case None => (None, None)
			case Some(n) if n.next.isEmpty => (Some(n), Some(n))
			case Some(n) =>
				var fast = n; var slow = n
				while (fast.next.isDefined && fast.next.get.next.isDefined) {
					slow = slow.next.get; fast = fast.next.get.next.get
				}
				val half = slow.next
				slow.next = None; half.foreach(_.prev = None)

				val (lh, _) = mergeSortRec(Some(n), cmp)
				val (rh, _) = mergeSortRec(half, cmp)
				mergeRanges(lh, rh, cmp)
		}
	}

	private def mergeRanges(left: Option[Node], right: Option[Node], cmp: Comparator[? >: T]): (Option[Node], Option[Node]) = {
		val dummy = new Node(null.asInstanceOf[T])
		var cur = dummy; var l = left; var r = right

		while (l.isDefined && r.isDefined) {
			if (cmp.compare(l.get.data, r.get.data) <= 0) {
				cur.next = l; l.get.prev = Some(cur); l = l.get.next
			} else {
				cur.next = r; r.get.prev = Some(cur); r = r.get.next
			}
			cur = cur.next.get
		}
		val rest = if (l.isDefined) l else r
		if (rest.isDefined) {
			cur.next = rest; rest.get.prev = Some(cur)
			while (cur.next.isDefined) cur = cur.next.get
		}
		val newHead = dummy.next
		newHead.foreach(_.prev = None)
		(newHead, Some(cur))
	}

	override def save(out: Writer): Unit = {
		val mapper = XmlMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build()
		// XmlMapper сам обойдет список через Iterator
		mapper.writeValue(out, this)
	}

	override def load(in: Reader, elementType: Class[? <: T]): Unit = {
		val mapper = XmlMapper.builder().build()
		val rootNode = mapper.readTree(in)

		this.clear()
		val it = rootNode.values().iterator()
		while (it.hasNext) {
			val node = it.next
			val element = mapper.treeToValue(node, elementType)
			this.add(element)
		}
	}
}
