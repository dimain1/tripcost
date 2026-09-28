package ru.tripcost.model.request;

/**
 * Базовое состояние: по умолчанию любое действие недопустимо. Конкретные состояния
 * переопределяют только те действия, которые в них разрешены.
 */
public abstract class AbstractRequestState implements RequestState {

    @Override
    public void submit(TripRequest request) {
        deny(RequestAction.SUBMIT);
    }

    @Override
    public void approve(TripRequest request) {
        deny(RequestAction.APPROVE);
    }

    @Override
    public void reject(TripRequest request, String reason) {
        deny(RequestAction.REJECT);
    }

    @Override
    public void pay(TripRequest request) {
        deny(RequestAction.PAY);
    }

    @Override
    public void complete(TripRequest request) {
        deny(RequestAction.COMPLETE);
    }

    @Override
    public void cancel(TripRequest request) {
        deny(RequestAction.CANCEL);
    }

    @Override
    public void revise(TripRequest request) {
        deny(RequestAction.REVISE);
    }

    private void deny(RequestAction action) {
        throw new IllegalStateException("В состоянии «" + name() + "» действие «" + action.title()
                + "» недоступно");
    }

    @Override
    public String toString() {
        return name();
    }
}
