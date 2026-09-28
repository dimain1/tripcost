package ru.tripcost.model.request;

import java.time.LocalTime;

/**
 * Событие перехода заявки из одного состояния в другое.
 */
public record StateChange(TripRequest request, RequestState from, RequestState to, String comment,
                          LocalTime time) {

    @Override
    public String toString() {
        return time + "  №" + request.id() + ": " + from.name() + " → " + to.name()
                + (comment.isEmpty() ? "" : " (" + comment + ")");
    }
}
