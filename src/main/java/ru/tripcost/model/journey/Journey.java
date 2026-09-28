package ru.tripcost.model.journey;

import ru.tripcost.model.domain.TransportType;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Составной маршрут: последовательность участков.
 *
 * <p><b>Паттерн «Итератор» (Iterator), роль Aggregate.</b> Участки хранятся во
 * внутреннем массиве, который наружу не выдаётся. Для обхода маршрут создаёт
 * итераторы: прямой (по порядку поездки), обратный (путь домой: участки в обратном
 * порядке и в обратном направлении) и с фильтром по виду транспорта. Итераторы
 * «быстро отказывают»: если маршрут изменился во время обхода, следующий
 * {@code next()} выбросит {@link ConcurrentModificationException}.</p>
 *
 * <p>Методы изменения вызываются командами (паттерн «Команда»), для которых
 * маршрут - получатель (Receiver).</p>
 *
 * <p><b>Паттерн «Снимок» (Memento), роль Originator.</b> Метод {@link #save(String)}
 * сохраняет состояние маршрута в объект {@link Memento}, а {@link #restore(Memento)}
 * возвращает маршрут к нему. Содержимое снимка доступно только самому маршруту.</p>
 */
public class Journey implements Iterable<Leg> {

    private Leg[] legs = new Leg[4];
    private int size;
    /** Счётчик изменений: по нему итераторы замечают изменение маршрута. */
    private int modCount;

    // ------------------------------------------------------------ изменение (для команд)

    public void add(Leg leg) {
        insert(size, leg);
    }

    public void insert(int index, Leg leg) {
        Objects.requireNonNull(leg);
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(index);
        }
        if (size == legs.length) {
            legs = Arrays.copyOf(legs, legs.length * 2);
        }
        System.arraycopy(legs, index, legs, index + 1, size - index);
        legs[index] = leg;
        size++;
        modCount++;
    }

    public Leg remove(int index) {
        Leg removed = get(index);
        System.arraycopy(legs, index + 1, legs, index, size - index - 1);
        legs[--size] = null;
        modCount++;
        return removed;
    }

    public Leg set(int index, Leg leg) {
        Leg old = get(index);
        legs[index] = Objects.requireNonNull(leg);
        modCount++;
        return old;
    }

    public void swap(int i, int j) {
        Leg a = get(i);
        legs[i] = get(j);
        legs[j] = a;
        modCount++;
    }

    // ------------------------------------------------------------ чтение

    public Leg get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
        return legs[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    // ------------------------------------------------------------ снимки

    /** Снимок текущего состояния маршрута. */
    public Memento save(String name) {
        return new Memento(name, Arrays.copyOf(legs, size));
    }

    /** Возвращает маршрут к состоянию из снимка. */
    public void restore(Memento memento) {
        Leg[] saved = memento.legs;
        legs = Arrays.copyOf(saved, Math.max(4, saved.length));
        size = saved.length;
        modCount++;
    }

    /**
     * Снимок маршрута. Участки (неизменяемые записи {@link Leg}) хранятся
     * в закрытом поле, прочитать которое может только {@link Journey}; снаружи
     * видны только название, число участков и время создания.
     */
    public static final class Memento {
        private final String name;
        private final Leg[] legs;
        private final LocalTime createdAt = LocalTime.now().withNano(0);

        private Memento(String name, Leg[] legs) {
            this.name = Objects.requireNonNull(name);
            this.legs = legs;
        }

        public String name() {
            return name;
        }

        public int size() {
            return legs.length;
        }

        public LocalTime createdAt() {
            return createdAt;
        }

        @Override
        public String toString() {
            return name + " (" + legs.length + " уч., " + createdAt + ")";
        }
    }

    // ------------------------------------------------------------ итераторы

    /** Участки по порядку поездки. */
    @Override
    public LegIterator iterator() {
        return new ForwardIterator();
    }

    /** Путь обратно: участки с конца, каждый в обратном направлении. */
    public LegIterator returnIterator() {
        return new ReturnIterator();
    }

    /** Только участки на указанном виде транспорта. */
    public LegIterator iterator(TransportType transport) {
        return new TransportFilterIterator(iterator(), transport);
    }

    /** Список участков - копия, изменение которой не влияет на маршрут. */
    public List<Leg> toList() {
        List<Leg> list = new ArrayList<>(size);
        for (Leg leg : this) {
            list.add(leg);
        }
        return list;
    }

    /** Прямой итератор. */
    private class ForwardIterator implements LegIterator {
        private int cursor;
        private final int expectedModCount = modCount;

        @Override
        public boolean hasNext() {
            return cursor < size;
        }

        @Override
        public Leg next() {
            checkForModification(expectedModCount);
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return legs[cursor++];
        }

        @Override
        public int number() {
            return cursor;
        }
    }

    /** Обратный итератор: от последнего участка к первому, направление меняется. */
    private class ReturnIterator implements LegIterator {
        private int cursor = size - 1;
        private final int expectedModCount = modCount;

        @Override
        public boolean hasNext() {
            return cursor >= 0;
        }

        @Override
        public Leg next() {
            checkForModification(expectedModCount);
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return legs[cursor--].reversed();
        }

        @Override
        public int number() {
            return size - cursor - 1;
        }
    }

    private void checkForModification(int expected) {
        if (modCount != expected) {
            throw new ConcurrentModificationException("Маршрут изменился во время обхода");
        }
    }
}
