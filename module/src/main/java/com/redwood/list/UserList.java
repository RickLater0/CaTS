package com.redwood.list;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.databind.JavaType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class UserList<E> extends AbstractList<E>  {

    private static class Node<E>{
        E data;
        Node<E> prev;
        Node<E> next;
        Node(E data){
            this.prev = null;
            this.next = null;
            this.data = data;
        }
    }
    @JsonIgnore
    private Node<E> head;
    @JsonIgnore
    private Node<E> tail;
    private int size;

    public UserList(Collection<? extends E> collection) {
        super();
        if (collection != null && !collection.isEmpty()) {
            addAll(collection);
        }
    }

    public UserList() {
        super();
    }

    private Node<E> getNode(int index) {
        if(index < 0 || index >= size)
            throw new IndexOutOfBoundsException();
        Node<E> cur;
        if(index <= size / 2){
            cur = head;
            for (int i = 0; i < index; i++) {
                cur = cur.next;
            }
        }else{
            cur = tail;
            for (int i = size - 1; i > index; i--) {
                cur = cur.prev;
            }
        }

        return cur;
    }

    @Override
    public boolean add(E e){
        add(size, e);
        return true;
    }

    private E rm(Node<E> cur){
        E data = cur.data;
        if (cur.prev != null)
            cur.prev.next = cur.next;
        else
            head = cur.next;

        if (cur.next != null)
            cur.next.prev = cur.prev;
        else
            tail = cur.prev;

        cur.prev = null;
        cur.next = null;
        cur.data = null;

        size--;
        modCount++;
        return data;
    }

    @Override
    public boolean remove(Object o) {
        var cur = head;
        while (cur != null) {
            if (Objects.equals(cur.data, o)) {
                rm(cur);
                return true;
            }
            cur = cur.next;
        }
        return false;
    }

    @Override
    public void clear() {
        Node<E> cur = head;
        while (cur != null) {
            Node<E> next = cur.next;
            cur.data = null;
            cur.prev = null;
            cur.next = null;
            cur = next;
        }
        head = tail = null;
        size = 0;
        modCount++;
    }

    @Override
    public E get(int index) {
        return getNode(index).data;
    }

    @Override
    public E set(int index, E element) {
        var node = getNode(index);
        var old = node.data;
        node.data = element;
        return old;
    }

    @Override
    public void add(int index, E element) {
        Node<E> node = new Node<>(element);
        if(index == 0){
            if (head == null) {
                head = node;
                tail = node;
            } else {
                node.next = head;
                head.prev = node;
                head = node;
            }
        } else if (index == size) {
            node.prev = tail;
            tail.next = node;
            tail = node;
        }else{
            Node<E> nd = getNode(index);
            Node<E> prev = nd.prev;
            node.prev = prev;
            node.next = nd;
            prev.next = node;
            nd.prev = node;
        }
        size++;
        modCount++;
    }

    @Override
    public E remove(int index){
        return rm(getNode(index));
    }

    @Override
    public int indexOf(Object o) {
        int index = 0;
        var cur = head;
        while (cur != null) {
            if (Objects.equals(cur.data, o)) {
                return index;
            }
            cur = cur.next;
            index++;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int index = size - 1;
        var cur = tail;
        while (cur != null) {
            if (Objects.equals(cur.data, o)) {
                return index;
            }
            cur = cur.prev;
            index--;
        }
        return -1;
    }

    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        if (index < 0 || index > size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);

        return new ListIterator<>() {
            private Node<E> next = (index == size) ? null : getNode(index);
            private Node<E> lastReturned = null;
            private int nextIdx = index;
            private int expectedModCount = modCount;

            private void checkForComodification() {
                if (modCount != expectedModCount)
                    throw new ConcurrentModificationException();
            }

            @Override
            public boolean hasNext() {
                return nextIdx < size;
            }

            @Override
            public E next() {
                checkForComodification();
                if (!hasNext())
                    throw new NoSuchElementException();

                lastReturned = next;
                next = next.next;
                nextIdx++;
                return lastReturned.data;
            }

            @Override
            public boolean hasPrevious() {
                return nextIdx > 0;
            }

            @Override
            public E previous() {
                checkForComodification();
                if (!hasPrevious())
                    throw new NoSuchElementException();

                next = (next == null) ? tail : next.prev;
                lastReturned = next;
                nextIdx--;
                return lastReturned.data;
            }

            @Override
            public int nextIndex() {
                return nextIdx;
            }

            @Override
            public int previousIndex() {
                return nextIdx - 1;
            }

            @Override
            public void remove() {
                checkForComodification();
                if (lastReturned == null)
                    throw new IllegalStateException();

                Node<E> lastNext = lastReturned.next;
                UserList.this.rm(lastReturned);

                if (next == lastReturned) {
                    next = lastNext;
                } else {
                    nextIdx--;
                }
                lastReturned = null;
                expectedModCount = modCount;
            }

            @Override
            public void set(E e) {
                checkForComodification();
                if (lastReturned == null)
                    throw new IllegalStateException();
                lastReturned.data = e;
            }

            @Override
            public void add(E e) {
                checkForComodification();
                UserList.this.add(nextIdx, e);
                lastReturned = null;
                nextIdx++;
                expectedModCount = modCount;
            }
        };
    }

    @Override
    public int size(){
        return size;
    }

    @Override
    public void forEach(Consumer<? super E> action){
        var cur = head;
        while(cur != null){
            action.accept(cur.data);
            cur = cur.next;
        }
    }

    public E firstThat(Predicate<E> predicate){
        var cur = head;
        while(cur != null){
            if(predicate.test(cur.data)){
                return cur.data;
            }
            cur = cur.next;
        }
        return null;
    }

    private record Range<E>(Node<E> head, Node<E> tail){}

    @Override
    public void sort(Comparator<? super E> cmp){
        if(size <= 1)
            return;

        if(cmp == null)
            throw new NullPointerException("Comparator cannot be null");

        var result = sortRec(head, cmp);
        head = result.head;
        tail = result.tail;
        modCount++;
    }

    private Range<E> sortRec(Node<E> node, Comparator<? super E> cmp){
        if (node == null)
            return new Range<>(null, null);
        if (node.next == null)
            return new Range<>(node, node);

        Node<E> fast = node;
        Node<E> slow = node;

        while(fast.next != null && fast.next.next != null){
            slow = slow.next;
            fast = fast.next.next;
        }

        Node<E> half = slow.next;
        slow.next = null;
        if(half != null){
            half.prev = null;
        }

        var left = sortRec(node, cmp);
        var right = sortRec(half, cmp);

        return merge(left, right, cmp);
    }

    private Range<E> merge(Range<E> leftRange, Range<E> rightRange, Comparator<? super E> cmp){
        Node<E> dummy = new Node<>(null);
        Node<E> cur = dummy;

        Node<E> left = leftRange.head;
        Node<E> right = rightRange.head;

        while(left != null && right != null){
            if(cmp.compare(left.data, right.data) <= 0){
                cur.next = left;
                left.prev = cur;
                left = left.next;
            }else{
                cur.next = right;
                right.prev = cur;
                right = right.next;
            }
            cur = cur.next;
        }

        Node<E> rest = (left != null) ? left : right;
        if (rest != null) {
            cur.next = rest;
            rest.prev = cur;
            while (cur.next != null)
                cur = cur.next;
        }

        Node<E> newHead = dummy.next;
        if (newHead != null)
            newHead.prev = null;
        return new Range<>(newHead, cur);
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size];
        int i = 0;
        for (Node<E> cur = head; cur != null; cur = cur.next) {
            arr[i++] = cur.data;
        }
        return arr;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            a = (T[]) Array.newInstance(a.getClass().getComponentType(), size);
        }
        int i = 0;
        for (Node<E> cur = head; cur != null; cur = cur.next) {
            a[i++] = (T) cur.data;
        }
        if (a.length > size) {
            a[size] = null;
        }
        return a;
    }

    public static<T> void save(Writer out, UserList<T> list) throws IOException{
        XmlMapper mapper = new XmlMapper();
        mapper.writeValue(out, list);
    }

    public static<T> UserList<T> load(Reader in, Class<T> eType) throws IOException {
        XmlMapper mapper = new XmlMapper();
        JavaType type = mapper.getTypeFactory().constructCollectionType(UserList.class, eType);
        return mapper.readValue(in, type);
    }
}
