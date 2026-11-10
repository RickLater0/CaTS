package com.redwood.collections_api;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Comparator;
import java.util.ListIterator;

public interface IUserList<E> extends Iterable<E> {
	int size();
	boolean add(E e);
	void add(int index, E e);
	E get(int index);
	E set(int index, E e);
	E remove(int index);
	boolean remove(Object o);
	void clear();
	void sort(Comparator<? super E> c);
	ListIterator<E> listIterator();

	void save(Writer out) throws IOException;
	void load(Reader in, Class<? extends E> elementType) throws IOException;
}
