package ru.tripcost.model.journey;

import ru.tripcost.model.domain.TransportType;

import java.util.Objects;

/**
 * Участок маршрута: откуда, куда и на чём.
 */
public record Leg(String from, String to, TransportType transport) {

    public Leg {
        from = Objects.requireNonNull(from).trim();
        to = Objects.requireNonNull(to).trim();
        Objects.requireNonNull(transport);
    }

    /** Тот же участок в обратном направлении. */
    public Leg reversed() {
        return new Leg(to, from, transport);
    }

    public Leg withTransport(TransportType type) {
        return new Leg(from, to, type);
    }

    @Override
    public String toString() {
        return from + " → " + to + " (" + transport + ")";
    }
}
