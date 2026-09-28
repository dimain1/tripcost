package ru.tripcost.model.journey;

import ru.tripcost.model.domain.TransportType;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Итератор с фильтром: пропускает участки на другом виде транспорта.
 * Работает поверх любого другого итератора маршрута.
 */
public class TransportFilterIterator implements LegIterator {

    private final LegIterator source;
    private final TransportType transport;
    private Leg nextLeg;
    private int nextNumber;
    private int number;

    public TransportFilterIterator(LegIterator source, TransportType transport) {
        this.source = Objects.requireNonNull(source);
        this.transport = Objects.requireNonNull(transport);
        advance();
    }

    @Override
    public boolean hasNext() {
        return nextLeg != null;
    }

    @Override
    public Leg next() {
        if (nextLeg == null) {
            throw new NoSuchElementException();
        }
        Leg result = nextLeg;
        number = nextNumber;
        advance();
        return result;
    }

    /** Номер участка в исходном маршруте. */
    @Override
    public int number() {
        return number;
    }

    private void advance() {
        nextLeg = null;
        while (source.hasNext()) {
            Leg leg = source.next();
            if (leg.transport() == transport) {
                nextLeg = leg;
                nextNumber = source.number();
                return;
            }
        }
    }
}
