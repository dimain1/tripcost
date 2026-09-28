package ru.tripcost.model.booking;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Общая часть имитации удалённого сервиса: номера операций, действующие брони
 * и переключатель сбоя для демонстрации компенсаций.
 */
abstract class RemoteService {

    private final String prefix;
    private final Map<String, String> active = new LinkedHashMap<>();
    private int counter;
    private boolean failing;

    protected RemoteService(String prefix) {
        this.prefix = prefix;
    }

    /** Включить имитацию сбоя: следующие запросы будут отклоняться. */
    public void setFailing(boolean failing) {
        this.failing = failing;
    }

    public boolean isFailing() {
        return failing;
    }

    /** Действующие (не отменённые) операции: номер → описание. */
    public Map<String, String> active() {
        return Map.copyOf(active);
    }

    protected String register(String description, String failureReason) {
        if (failing) {
            throw new ServiceException(failureReason);
        }
        String id = prefix + "-" + (++counter);
        active.put(id, description);
        return id;
    }

    protected void unregister(String id) {
        active.remove(id);
    }
}
