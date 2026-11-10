package com.redwood.collections.kotlin

import com.redwood.collections_api.IUserList
import tools.jackson.databind.SerializationFeature
import tools.jackson.dataformat.xml.XmlMapper
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.util.*

class UserKotlinList<T> : IUserList<T> {
    private val delegate = ArrayList<T>()

    override fun size(): Int = delegate.size

    override fun get(index: Int): T = delegate[index]

    override fun set(index: Int, element: T): T {
        return delegate.set(index, element)
    }

    override fun add(element: T): Boolean = delegate.add(element)

    override fun add(index: Int, element: T) {
        delegate.add(index, element)
    }

    override fun remove(index: Int): T = delegate.removeAt(index)

    override fun remove(o: Any?): Boolean = delegate.remove(o)

    override fun clear() {
        delegate.clear()
    }

    // Исправленный метод sort
    override fun sort(c: Comparator<in T>?) {
        if (c == null) {
            throw NullPointerException("Comparator cannot be null")
        }
        delegate.sortWith(c)
    }

    override fun iterator(): MutableIterator<T> = delegate.iterator()

    override fun listIterator(): MutableListIterator<T> = delegate.listIterator()

    @Throws(IOException::class)
    override fun save(out: Writer) {
        val mapper = XmlMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build()
        mapper.writeValue(out, this)
    }

    @Throws(IOException::class)
    override fun load(input: Reader, elementType: Class<out T>) {
        val mapper = XmlMapper.builder().build()
        this.clear()

        // Потоковое чтение для избежания промежуточной коллекции
        mapper.readerFor(elementType).readValues<T>(input).use { iter ->
            while (iter.hasNext()) {
                this.add(iter.next())
            }
        }
    }
}
