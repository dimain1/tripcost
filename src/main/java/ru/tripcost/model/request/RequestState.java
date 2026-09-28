package ru.tripcost.model.request;

import java.util.Set;

/**
 * Состояние заявки.
 *
 * <p><b>Паттерн «Состояние» (State).</b> Заявка делегирует каждое действие своему
 * текущему состоянию. Состояние выполняет действие (и, если нужно, переводит
 * заявку в другое состояние) или отказывает, если в нём действие недопустимо.</p>
 */
public interface RequestState {

    String name();

    /** Действия, доступные в этом состоянии (для кнопок интерфейса). */
    Set<RequestAction> allowedActions();

    void submit(TripRequest request);

    void approve(TripRequest request);

    void reject(TripRequest request, String reason);

    void pay(TripRequest request);

    void complete(TripRequest request);

    void cancel(TripRequest request);

    void revise(TripRequest request);

    /** Конечное состояние: из него переходов нет. */
    default boolean isFinal() {
        return allowedActions().isEmpty();
    }
}
