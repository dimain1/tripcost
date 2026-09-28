package ru.tripcost.model.journey;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Хранитель снимков маршрута: сохранённые варианты маршрута по названиям.
 *
 * <p><b>Паттерн «Снимок» (Memento), роль Caretaker.</b> Хранит снимки и отдаёт их
 * для восстановления, но не знает, что внутри снимка, и не может его изменить.</p>
 */
public class JourneyVersions {

    private final List<Journey.Memento> versions = new ArrayList<>();

    /** Сохраняет вариант; вариант с тем же названием заменяется. */
    public Journey.Memento save(Journey journey, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Введите название варианта маршрута");
        }
        if (journey.isEmpty()) {
            throw new IllegalArgumentException("Маршрут пуст: сохранять нечего");
        }
        Journey.Memento memento = journey.save(name.trim());
        versions.removeIf(v -> v.name().equalsIgnoreCase(memento.name()));
        versions.add(memento);
        return memento;
    }

    public Optional<Journey.Memento> find(String name) {
        return versions.stream().filter(v -> v.name().equalsIgnoreCase(name)).findFirst();
    }

    public List<Journey.Memento> list() {
        return List.copyOf(versions);
    }

    public void remove(Journey.Memento memento) {
        versions.remove(memento);
    }
}
