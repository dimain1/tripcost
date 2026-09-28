package ru.tripcost.model.data;

import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripBuilder;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Сохранённый пользователем шаблон поездки: название, поездка и время сохранения.
 */
public record SavedTrip(String name, Trip trip, LocalDateTime savedAt) {

    public SavedTrip {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Не указано название шаблона");
        }
        name = name.trim();
        Objects.requireNonNull(trip);
        Objects.requireNonNull(savedAt);
    }

    public SavedTrip(String name, Trip trip) {
        this(name, trip, LocalDateTime.now().withNano(0));
    }

    /** Строитель для реестра шаблонов (паттерн «Прототип»). */
    public TripBuilder toBuilder() {
        return trip.toBuilder();
    }
}
