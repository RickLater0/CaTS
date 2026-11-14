package com.redwood.collections.scala

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

import java.util
import java.util.stream.Stream as JStream
import java.util.{ArrayList, List as JList}
import java.util.concurrent.atomic.AtomicLong
import scala.jdk.CollectionConverters.*
import scala.util.Random

object UserScalaListTest {
	def functionalCases(): JStream[JList[Int]] = {
		val cases = List(
			List(), List(42), List(1, 2, 3, 4, 5), List(5, 4, 3, 2, 1),
			List(3, 1, 4, 5, 2), List(7, 7, 7, 7, 7), List(3, 1, 3, 2, 3),
			List(1, 1, 3, 3, 2, 2, 5, 5), List(5, 2, 9, 1, 3), List(9, 1, 3, 2, 4),
			List(1, 3, 2, 4, 9), List(9, 1, 9, 2, 9, 3)
		)
		val jlists = cases.map { c =>
			val al = new util.ArrayList[Int]()
			c.foreach(al.add)
			al
		}
		JStream.of(jlists*)
	}

	/**
	 * Безопасный доступ к приватным полям Scala (учитывает маскировку имен компилятором).
	 */
	def getField(obj: AnyRef, name: String): Option[AnyRef] = {
		var c: Class[?] = obj.getClass
		while (c != null) {
			val fields = c.getDeclaredFields
			// Ищем поле, имя которого заканчивается на нужное (например, "head")
			val fOpt = fields.find(_.getName.endsWith(name))
			fOpt match {
				case Some(f) =>
					f.setAccessible(true)
					try {
						val v = f.get(obj)
						// Оборачиваем в Some, чтобы отличать отсутствие поля от null
						return Some(if (v == null) scala.None else v)
					} catch {
						case _: Throwable =>
					}
				case None =>
			}
			c = c.getSuperclass
		}
		None
	}

	/**
	 * Подсчет физического количества узлов через рефлексию
	 */
	def countNodes(list: UserScalaList[?]): Option[Int] = {
		getField(list, "head") match {
			case None => None
			case Some(headOpt) =>
				val headAny = headOpt.asInstanceOf[Option[AnyRef]]
				var n = 0
				var cur = headAny
				while (cur.isDefined) {
					n += 1
					val nextOpt = getField(cur.get, "next").getOrElse(scala.None).asInstanceOf[Option[AnyRef]]
					cur = nextOpt
				}
				Some(n)
		}
	}
}

class UserScalaListTest {

	import UserScalaListTest.*

	private def toList[T](list: UserScalaList[T]): List[T] =
		(0 until list.size()).map(list.get).toList

	private def cmpInt: (Int, Int) => Int = (a, b) => a.compareTo(b)

	// ============================================================
	// Функциональные тесты
	// ============================================================

	@ParameterizedTest(name = "{0}")
	@MethodSource(Array("com.redwood.collections.scala.UserScalaListTest#functionalCases"))
	def sortImperativeMatchesReference(input: JList[Int]): Unit = {
		val list = new UserScalaList[Int]()
		input.forEach(v => list.add(v))
		list.sortImperative(cmpInt)
		assertEquals(input.asScala.sorted.toList, toList(list))
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource(Array("com.redwood.collections.scala.UserScalaListTest#functionalCases"))
	def sortFunctionalMatchesReference(input: JList[Int]): Unit = {
		val list = new UserScalaList[Int]()
		input.forEach(v => list.add(v))
		list.sortFunctional(cmpInt)
		assertEquals(input.asScala.sorted.toList, toList(list))
	}

	@Test
	def emptyAndSingleElementDoNotCallComparator(): Unit = {
		val exploding = (a: Int, b: Int) => throw new AssertionError("Must not be called")
		val empty = new UserScalaList[Int]()
		empty.sortImperative(exploding)
		empty.sortFunctional(exploding)
		val single = new UserScalaList[Int]()
		single.add(1)
		single.sortImperative(exploding)
		single.sortFunctional(exploding)
	}

	@Test
	def nullComparatorRejectedForNonEmptyList(): Unit = {
		val list = new UserScalaList[Int]()
		list.add(1); list.add(2)
		assertThrows(classOf[NullPointerException], () => {
			list.sort(null.asInstanceOf[java.util.Comparator[? >: Int]])
		})
	}

	@Test
	def mergeSortIsStableForEqualKeysImperative(): Unit = {
		case class Keyed(key: Int, id: String)
		val list = new UserScalaList[Keyed]()
		list.add(Keyed(2, "a")); list.add(Keyed(1, "b")); list.add(Keyed(2, "c"))
		list.sortImperative((a: Keyed, b: Keyed) => a.key.compareTo(b.key))
		assertEquals("b", list.get(0).id)
		assertEquals("a", list.get(1).id)
		assertEquals("c", list.get(2).id)
	}

	@Test
	def mergeSortIsStableForEqualKeysFunctional(): Unit = {
		case class Keyed(key: Int, id: String)
		val list = new UserScalaList[Keyed]()
		list.add(Keyed(2, "a")); list.add(Keyed(1, "b")); list.add(Keyed(2, "c"))
		list.sortFunctional((a: Keyed, b: Keyed) => a.key.compareTo(b.key))
		assertEquals("b", list.get(0).id)
		assertEquals("a", list.get(1).id)
		assertEquals("c", list.get(2).id)
	}

	@Test
	def randomDataAcrossSizesImperative(): Unit = {
		val rnd = new Random(42)
		for (n <- Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 17, 32, 33)) {
			val list = new UserScalaList[Int]()
			val data = (0 until n).map(_ => rnd.nextInt(21) - 10).toList
			data.foreach(v => list.add(v))
			list.sortImperative(cmpInt)
			assertEquals(data.sorted, toList(list), s"Size: $n")
		}
	}

	@Test
	def randomDataAcrossSizesFunctional(): Unit = {
		val rnd = new Random(42)
		for (n <- Array(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 17, 32, 33)) {
			val list = new UserScalaList[Int]()
			val data = (0 until n).map(_ => rnd.nextInt(21) - 10).toList
			data.foreach(v => list.add(v))
			list.sortFunctional(cmpInt)
			assertEquals(data.sorted, toList(list), s"Size: $n")
		}
	}

	// ============================================================
	// Структурное покрытие
	// ============================================================

	@Test
	def structuralBaseCases(): Unit = {
		val e1 = new UserScalaList[Int](); e1.sortImperative(cmpInt)
		val e2 = new UserScalaList[Int](); e2.sortFunctional(cmpInt)
		assertEquals(0, e1.size()); assertEquals(0, e2.size())
		val s1 = new UserScalaList[Int](); s1.add(7); s1.sortImperative(cmpInt)
		val s2 = new UserScalaList[Int](); s2.add(7); s2.sortFunctional(cmpInt)
		assertEquals(List(7), toList(s1)); assertEquals(List(7), toList(s2))
	}

	@Test
	def structuralEvenSizeSplit(): Unit = {
		val l1 = new UserScalaList[Int](); List(4, 3, 2, 1).foreach(l1.add); l1.sortImperative(cmpInt)
		val l2 = new UserScalaList[Int](); List(4, 3, 2, 1).foreach(l2.add); l2.sortFunctional(cmpInt)
		assertEquals(List(1, 2, 3, 4), toList(l1))
		assertEquals(List(1, 2, 3, 4), toList(l2))
	}

	@Test
	def structuralOddSizeSplit(): Unit = {
		val l1 = new UserScalaList[Int](); List(5, 4, 3, 2, 1).foreach(l1.add); l1.sortImperative(cmpInt)
		val l2 = new UserScalaList[Int](); List(5, 4, 3, 2, 1).foreach(l2.add); l2.sortFunctional(cmpInt)
		assertEquals(List(1, 2, 3, 4, 5), toList(l1))
		assertEquals(List(1, 2, 3, 4, 5), toList(l2))
	}

	@Test
	def structuralMergeBranches(): Unit = {
		// less
		val l1 = new UserScalaList[Int](); List(1, 2, 3, 4).foreach(l1.add); l1.sortImperative(cmpInt)
		assertEquals(List(1, 2, 3, 4), toList(l1))
		// equal
		val l2 = new UserScalaList[Int](); List(2, 1, 2, 1).foreach(l2.add); l2.sortFunctional(cmpInt)
		assertEquals(List(1, 1, 2, 2), toList(l2))
		// greater
		val l3 = new UserScalaList[Int](); List(3, 4, 1, 2).foreach(l3.add); l3.sortImperative(cmpInt)
		assertEquals(List(1, 2, 3, 4), toList(l3))
	}

	@Test
	def structuralComparisonCountWithinNLogNBounds(): Unit = {
		for (n <- Array(64, 128, 256, 512, 1024)) {
			for (impl <- 0 to 1) {
				val cnt = new AtomicLong()
				val cmp = (a: Int, b: Int) => { cnt.incrementAndGet(); a.compareTo(b) }
				val list = new UserScalaList[Int]()
				val rnd = new Random(n.toLong)
				for (_ <- 0 until n) list.add(rnd.nextInt())
				if (impl == 0) list.sortImperative(cmp) else list.sortFunctional(cmp)
				val log2n = (math.log(n.toDouble) / math.log(2.0)).toLong
				val lower = math.max(1L, n.toLong * log2n / 3)
				val upper = n.toLong * (log2n + 2)
				assertTrue(cnt.get() >= lower,
					s"impl=$impl n=$n cmp=${cnt.get()} < $lower")
				assertTrue(cnt.get() <= upper,
					s"impl=$impl n=$n cmp=${cnt.get()} > $upper")
			}
		}
	}

	// ============================================================
	// Полупрозрачный ящик
	// ============================================================

	@Test
	def whiteBoxHeadPrevTailNextNullBothSorts(): Unit = {
		for (impl <- 0 to 1) {
			val list = new UserScalaList[Int]()
			List(3, 1, 4, 1, 5).foreach(list.add)
			if (impl == 0) list.sortImperative(cmpInt) else list.sortFunctional(cmpInt)

			val headOpt = getField(list, "head").getOrElse(scala.None).asInstanceOf[Option[AnyRef]]
			val tailOpt = getField(list, "tail").getOrElse(scala.None).asInstanceOf[Option[AnyRef]]

			assertTrue(headOpt.isDefined, s"impl=$impl head defined")
			assertTrue(tailOpt.isDefined, s"impl=$impl tail defined")

			val headPrev = getField(headOpt.get, "prev").getOrElse(scala.None).asInstanceOf[Option[AnyRef]]
			val tailNext = getField(tailOpt.get, "next").getOrElse(scala.None).asInstanceOf[Option[AnyRef]]

			assertTrue(headPrev.isEmpty, s"impl=$impl head.prev must be None")
			assertTrue(tailNext.isEmpty, s"impl=$impl tail.next must be None")
		}
	}

	@Test
	def whiteBoxSizeAndNodeCountPreserved(): Unit = {
		for (impl <- 0 to 1) {
			for (n <- Array(0, 1, 2, 5, 17, 100)) {
				val list = new UserScalaList[Int]()
				val rnd = new Random(n.toLong)
				for (_ <- 0 until n) list.add(rnd.nextInt())
				if (impl == 0) list.sortImperative(cmpInt) else list.sortFunctional(cmpInt)

				assertEquals(n, list.size())
				val nodes = countNodes(list)
				assertTrue(nodes.isDefined, s"impl=$impl reflection available")
				assertEquals(n, nodes.get)
			}
		}
	}

	@Test
	def whiteBoxBidirectionalTraversalConsistent(): Unit = {
		for (impl <- 0 to 1) {
			val list = new UserScalaList[Int]()
			List(5, 1, 4, 1, 3, 5, 2).foreach(list.add)
			if (impl == 0) list.sortImperative(cmpInt) else list.sortFunctional(cmpInt)
			val fwd = toList(list)
			val it = list.listIterator(list.size())
			val bwd = scala.collection.mutable.ListBuffer.empty[Int]
			while (it.hasPrevious) bwd += it.previous()
			assertEquals(fwd, bwd.reverse.toList, s"impl=$impl")
		}
	}

	@Test
	def whiteBoxElementsPreservedAsMultiset(): Unit = {
		for (impl <- 0 to 1) {
			val data = List(3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5)
			val list = new UserScalaList[Int]()
			data.foreach(list.add)
			if (impl == 0) list.sortImperative(cmpInt) else list.sortFunctional(cmpInt)
			assertEquals(data.sorted, toList(list), s"impl=$impl")
		}
	}

	// ============================================================
	// Обнаружение дефектов
	// ============================================================

	@Test
	def defectHeadPrevNotReset(): Unit = {
		for (impl <- 0 to 1) {
			val list = new UserScalaList[Int]()
			List(4, 3, 2, 1).foreach(list.add)
			if (impl == 0) list.sortImperative(cmpInt) else list.sortFunctional(cmpInt)

			val headOpt = getField(list, "head").getOrElse(scala.None).asInstanceOf[Option[AnyRef]]
			assertTrue(headOpt.isDefined, s"impl=$impl head defined")

			val prev = getField(headOpt.get, "prev").getOrElse(scala.None).asInstanceOf[Option[AnyRef]]
			assertTrue(prev.isEmpty, s"impl=$impl head.prev must be None")
		}
	}

	@Test
	def defectTailNextNotReset(): Unit = {
		for (impl <- 0 to 1) {
			val list = new UserScalaList[Int]()
			List(4, 3, 2, 1).foreach(list.add)
			if (impl == 0) list.sortImperative(cmpInt) else list.sortFunctional(cmpInt)

			val tailOpt = getField(list, "tail").getOrElse(scala.None).asInstanceOf[Option[AnyRef]]
			assertTrue(tailOpt.isDefined, s"impl=$impl tail defined")

			val next = getField(tailOpt.get, "next").getOrElse(scala.None).asInstanceOf[Option[AnyRef]]
			assertTrue(next.isEmpty, s"impl=$impl tail.next must be None")
		}
	}

	@Test
	def defectUnstableMerge(): Unit = {
		case class Keyed(key: Int, id: String)
		for (impl <- 0 to 1) {
			val list = new UserScalaList[Keyed]()
			list.add(Keyed(1, "a")); list.add(Keyed(1, "b")); list.add(Keyed(1, "c"))
			if (impl == 0) list.sortImperative((a: Keyed, b: Keyed) => a.key.compareTo(b.key))
			else list.sortFunctional((a: Keyed, b: Keyed) => a.key.compareTo(b.key))
			assertEquals("a", list.get(0).id, s"impl=$impl")
			assertEquals("b", list.get(1).id, s"impl=$impl")
			assertEquals("c", list.get(2).id, s"impl=$impl")
		}
	}

	@Test
	def defectNoNullComparatorCheck(): Unit = {
		val list = new UserScalaList[Int]()
		list.add(1); list.add(2)
		assertThrows(classOf[NullPointerException], () => {
			list.sort(null.asInstanceOf[java.util.Comparator[? >: Int]])
		})
	}

	@Test
	def defectSizeOrContentCorruption(): Unit = {
		for (impl <- 0 to 1) {
			val data = List(2, 1, 4, 3, 6, 5, 8, 7)
			val list = new UserScalaList[Int]()
			data.foreach(list.add)
			if (impl == 0) list.sortImperative(cmpInt) else list.sortFunctional(cmpInt)
			assertEquals(data.sorted, toList(list), s"impl=$impl")
			assertEquals(data.size, list.size())
		}
	}

	// ============================================================
	// Производительность и память
	// ============================================================

	@Test
	def performanceComplexityIsNLogN(): Unit = {
		for (impl <- 0 to 1) {
			val counts = new Array[Long](2)
			val sizes = Array(512, 1024)
			for (k <- sizes.indices) {
				val n = sizes(k)
				val cnt = new AtomicLong()
				val cmp = (a: Int, b: Int) => { cnt.incrementAndGet(); a.compareTo(b) }
				val list = new UserScalaList[Int]()
				val rnd = new Random(n.toLong)
				for (_ <- 0 until n) list.add(rnd.nextInt())
				if (impl == 0) list.sortImperative(cmp) else list.sortFunctional(cmp)
				counts(k) = cnt.get()
			}
			val ratio = counts(1).toDouble / counts(0).toDouble
			assertTrue(ratio > 2.05, s"impl=$impl ratio too small: $ratio")
			assertTrue(ratio < 3.0, s"impl=$impl ratio too large: $ratio")
		}
	}

	@Test
	def performanceMemoryScalesLinearly(): Unit = {
		val n = 5000
		val list = new UserScalaList[Int]()
		for (i <- 0 until n) list.add(i)
		val nodes = countNodes(list)
		assertTrue(nodes.isDefined, "reflection on head must work")
		assertEquals(n, nodes.get)
		assertEquals(n, list.size())
	}
}
