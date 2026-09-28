package ru.tripcost.model.request;

import java.util.ArrayList;
import java.util.List;

/**
 * Наблюдатель: журнал всех переходов всех заявок.
 */
public class RequestJournal implements RequestListener {

    private final List<StateChange> entries = new ArrayList<>();

    @Override
    public void onStateChanged(StateChange change) {
        entries.add(change);
    }

    public List<StateChange> entries() {
        return List.copyOf(entries);
    }

    /** Все переходы одной заявки. */
    public List<StateChange> of(TripRequest request) {
        return entries.stream().filter(e -> e.request() == request).toList();
    }
}
