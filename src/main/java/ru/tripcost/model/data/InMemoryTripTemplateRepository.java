package ru.tripcost.model.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Репозиторий в памяти: данные живут до закрытия программы. Используется в тестах.
 */
public class InMemoryTripTemplateRepository implements TripTemplateRepository {

    private final Map<String, SavedTrip> items = new LinkedHashMap<>();

    @Override
    public List<SavedTrip> findAll() {
        return new ArrayList<>(items.values());
    }

    @Override
    public Optional<SavedTrip> findById(String name) {
        return Optional.ofNullable(items.get(name));
    }

    @Override
    public void save(SavedTrip entity) {
        items.put(entity.name(), entity);
    }

    @Override
    public boolean deleteById(String name) {
        return items.remove(name) != null;
    }

    @Override
    public long count() {
        return items.size();
    }
}
