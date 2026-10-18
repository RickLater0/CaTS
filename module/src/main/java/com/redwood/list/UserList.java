package com.redwood.list;

import com.fasterxml.jackson.annotation.JsonIgnore;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.databind.JavaType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Пользовательская реализация структуры данных «Двусвязный разомкнутый список»,
 * наследующаяся от {@link AbstractList}.
 * <p>
 * Особенности реализации:
 * <ul>
 *     <li>Элементы хранятся в узлах {@link Node}, содержащих ссылки на предыдущий и следующий элементы.</li>
 *     <li>Список является «разомкнутым»: {@code head.prev == null} и {@code tail.next == null}.</li>
 *     <li>Реализована сортировка слиянием (Merge Sort).</li>
 *     <li>Поддерживается сериализация в XML с использованием библиотеки Jackson.</li>
 * </ul>
 *
 * @param <E> тип хранимых элементов
 */
public class UserList<E> extends AbstractList<E>  {

    /**
     * Внутренний класс, представляющий узел двусвязного списка.
     *
     * @param <E> тип данных в узле
     */
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

    /**
     * Создает список и заполняет его элементами из переданной коллекции.
     *
     * @param collection коллекция, элементы которой будут скопированы в список
     */
    public UserList(Collection<? extends E> collection) {
        super();
        if (collection != null && !collection.isEmpty()) {
            addAll(collection);
        }
    }

    /// Создание пустого списка
    public UserList() {
        super();
    }

    /**
     * Возвращает узел по его логическому номеру (индексу).
     *
     * @param index индекс элемента
     * @return узел списка
     * @throws IndexOutOfBoundsException если индекс выходит за пределы списка
     */
    private Node<E> getNode(int index) {
        if(index < 0 || index >= size)
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
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

    /**
     * Добавляет элемент в конец списка.
     *
     * @param e добавляемый элемент
     * @return {@code true} (согласно контракту {@link Collection#add})
     */
    @Override
    public boolean add(E e){
        add(size, e);
        return true;
    }

    /**
     * Внутренний метод удаления узла из списка.
     * <p>
     * Разрывает связи узла с соседями и обнуляет данные.
     *
     * @param cur удаляемый узел
     * @return данные, которые хранились в узле
     */
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

    /**
     * Удаляет первое вхождение указанного объекта из списка.
     *
     * @param o объект для удаления
     * @return {@code true}, если элемент был найден и удален
     */
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

    /// Очищает список, удаляя все элементы.
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

    /**
     * Возвращает элемент по указанному индексу.
     *
     * @param index индекс элемента
     * @return элемент списка
     * @throws IndexOutOfBoundsException если индекс выходит за границы
     */
    @Override
    public E get(int index) {
        return getNode(index).data;
    }

    /**
     * Заменяет элемент по указанному индексу.
     *
     * @param index индекс заменяемого элемента
     * @param element новый элемент
     * @return предыдущий элемент, хранившийся по этому индексу
     */
    @Override
    public E set(int index, E element) {
        var node = getNode(index);
        var old = node.data;
        node.data = element;
        return old;
    }

    /**
     * Вставляет элемент по указанному индексу.
     *
     * @param index индекс, по которому будет вставлен элемент
     * @param element вставляемый элемент
     * @throws IndexOutOfBoundsException если индекс выходит за границы
     */
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

    /**
     * Удаляет элемент по указанному индексу.
     *
     * @param index индекс удаляемого элемента
     * @return удаленный элемент
     */
    @Override
    public E remove(int index){
        return rm(getNode(index));
    }

    /**
     * Возвращает индекс первого вхождения объекта.
     *
     * @param o искомый объект
     * @return индекс элемента или {@code -1}, если элемент не найден
     */
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

    /**
     * Возвращает индекс последнего вхождения объекта.
     *
     * @param o искомый объект
     * @return индекс элемента или {@code -1}, если элемент не найден
     */
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

    /**
     * Возвращает итератор списка (начиная с нулевого индекса).
     *
     * @return итератор списка
     */
    @Override
    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    /**
     * Возвращает двунаправленный итератор по списку.
     * <p>
     * Итератор поддерживает fail-fast поведение и выбрасывает {@link ConcurrentModificationException},
     * если список был изменен структурно во время итерации.
     *
     * @param index индекс, с которого начнется итерация
     * @return итератор списка
     */
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
                    throw new ConcurrentModificationException("Concurrent modification");
            }

            @Override
            public boolean hasNext() {
                return nextIdx < size;
            }

            @Override
            public E next() {
                checkForComodification();
                if (!hasNext())
                    throw new NoSuchElementException("No such element");

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
                    throw new NoSuchElementException("No previous element");

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
                    throw new IllegalStateException("Illegal state");

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
                    throw new IllegalStateException("Illegal state");
                lastReturned.data = e;
            }

            @Override
            public void add(E e) {
                checkForComodification();
                Node<E> newNode = new Node<>(e);

                if (next != null) {
                    newNode.prev = next.prev;
                    newNode.next = next;

                    if (next.prev == null) {
                        head = newNode;
                    } else {
                        next.prev.next = newNode;
                    }
                    next.prev = newNode;
                } else {
                    newNode.prev = tail;

                    if (tail == null) {
                        head = newNode;
                    } else {
                        tail.next = newNode;
                    }
                    tail = newNode;
                }

                nextIdx++;
                size++;
                modCount++;

                lastReturned = null;
                expectedModCount = modCount;
            }
        };
    }

    /**
     * Возвращает текущий размер списка.
     *
     * @return количество элементов в списке
     */
    @Override
    public int size(){
        return size;
    }

    //foreach есть через интерфейс Iterable

    /**
     * Выполняет заданное действие для каждого элемента списка.
     * <p>
     * Реализация кастомного итератора из задания (вместо стандартного {@code for-each}).
     *
     * @param action действие, выполняемое для каждого элемента
     *
     * @throws NullPointerException если action равен {@code null}
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        if(action == null)
            throw new NullPointerException("Action can not be null");
        var cur = head;
        while (cur != null) {
            action.accept(cur.data);
            cur = cur.next;
        }
    }

    /**
     * Находит первый элемент, удовлетворяющий заданному условию.
     * <p>
     * Реализация кастомного поиска из задания.
     *
     * @param predicate условие проверки элемента
     * @return первый найденный элемент или {@code null}, если элементов, удовлетворяющих условию, нет
     */
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

    /**
     * Сортирует список с использованием алгоритма сортировки слиянием (Merge Sort).
     * <p>
     * Сложность алгоритма: O(N log N). Сортировка устойчивая (stable).
     *
     * @param cmp компаратор для сравнения элементов
     * @throws NullPointerException если компаратор равен {@code null}
     */
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

    /**
     * Рекурсивно разделяет список на две половины и сортирует каждую.
     *
     * @param node начало сортируемой части
     * @param cmp компаратор
     * @return диапазон, содержащий начало и конец отсортированной части
     */
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

    /**
     * Сливает два отсортированных списка в один.
     *
     * @param leftRange левый отсортированный диапазон
     * @param rightRange правый отсортированный диапазон
     * @param cmp компаратор
     * @return объединенный отсортированный диапазон
     */
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

    /**
     * Возвращает массив, содержащий все элементы списка.
     *
     * @return массив объектов
     */
    @Override
    public Object[] toArray() {
        Object[] arr = new Object[size];
        int i = 0;
        for (Node<E> cur = head; cur != null; cur = cur.next) {
            arr[i++] = cur.data;
        }
        return arr;
    }

    /**
     * Возвращает массив, содержащий все элементы списка; тип возвращаемого массива
     * определяется типом переданного массива.
     *
     * @param a массив, в который будут помещены элементы (если достаточно места)
     * @return массив, содержащий все элементы списка
     */
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

    /**
     * Сохраняет список в формате XML в указанный поток вывода.
     * <p>
     * Использует Jackson {@code XmlMapper} с включенным форматированием (отступами).
     *
     * @param out поток для записи
     * @param list сохраняемый список
     * @param <T> тип хранимых элементов
     * @throws IOException при ошибке записи в поток
     */
    public static<T> void save(Writer out, UserList<T> list) throws IOException{
        XmlMapper mapper = XmlMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        mapper.writeValue(out, list);
    }

    /**
     * Загружает список из XML-потока.
     *
     * @param in поток для чтения
     * @param eType класс хранимых элементов (нужен для десериализации generic-типа)
     * @param <T> тип хранимых элементов
     * @return загруженный список
     * @throws RuntimeException если файл поврежден или не соответствует ожидаемому формату
     */
    public static<T> UserList<T> load(Reader in, Class<T> eType) throws RuntimeException{
        XmlMapper mapper = XmlMapper.builder().build();
        JavaType type = mapper.getTypeFactory().constructCollectionType(UserList.class, eType);
        return mapper.readValue(in, type);
    }
}
