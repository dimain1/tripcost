package ru.tripcost.model.request;

import ru.tripcost.model.approval.ApprovalDecision;
import ru.tripcost.model.approval.ApprovalHandler;
import ru.tripcost.model.approval.ApprovalRequest;
import ru.tripcost.model.booking.BookingOrder;
import ru.tripcost.model.booking.BookingSaga;
import ru.tripcost.model.booking.SagaResult;
import ru.tripcost.model.booking.TravelServices;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.pricing.CostBreakdown;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Заявка на командировку.
 *
 * <p><b>Паттерн «Состояние» (State), роль Context.</b> Заявка хранит ссылку на текущее
 * состояние и передаёт ему все действия; сама она условных операторов по состоянию
 * не содержит.</p>
 *
 * <p><b>Паттерн «Наблюдатель» (Observer), роль Subject.</b> Заявка хранит список
 * подписчиков и оповещает их о каждом переходе между состояниями.</p>
 */
public class TripRequest {

    private final int id;
    private final Trip trip;
    private final CostBreakdown cost;
    private final ApprovalHandler approvalChain;
    private final BookingSaga booking;
    private final List<RequestListener> listeners = new CopyOnWriteArrayList<>();

    private RequestState state = RequestStates.DRAFT;
    private ApprovalDecision decision;
    private SagaResult lastBooking;

    /** Заявка с отдельным набором сервисов бронирования (для тестов и примеров). */
    public TripRequest(int id, Trip trip, CostBreakdown cost, ApprovalHandler approvalChain) {
        this(id, trip, cost, approvalChain,
                BookingSaga.standard(TravelServices.create(new java.math.BigDecimal("1000000"))));
    }

    public TripRequest(int id, Trip trip, CostBreakdown cost, ApprovalHandler approvalChain,
                       BookingSaga booking) {
        this.id = id;
        this.trip = Objects.requireNonNull(trip);
        this.cost = Objects.requireNonNull(cost);
        this.approvalChain = Objects.requireNonNull(approvalChain);
        this.booking = Objects.requireNonNull(booking);
    }

    // ------------------------------------------------------------ наблюдатели

    public void addListener(RequestListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void removeListener(RequestListener listener) {
        listeners.remove(listener);
    }

    // ------------------------------------------------------------ действия: делегируются состоянию

    public void submit() {
        state.submit(this);
    }

    public void approve() {
        state.approve(this);
    }

    public void reject(String reason) {
        state.reject(this, reason);
    }

    public void pay() {
        state.pay(this);
    }

    public void complete() {
        state.complete(this);
    }

    public void cancel() {
        state.cancel(this);
    }

    public void revise() {
        state.revise(this);
    }

    /** Выполнить действие по его коду (для кнопок интерфейса). */
    public void perform(RequestAction action, String comment) {
        switch (action) {
            case SUBMIT -> submit();
            case APPROVE -> approve();
            case REJECT -> reject(comment);
            case PAY -> pay();
            case COMPLETE -> complete();
            case CANCEL -> cancel();
            case REVISE -> revise();
        }
    }

    // ------------------------------------------------------------ для состояний

    /** Переход в новое состояние и оповещение подписчиков. */
    void changeState(RequestState next, String comment) {
        RequestState previous = state;
        state = next;
        StateChange change = new StateChange(this, previous, next, comment == null ? "" : comment,
                LocalTime.now().withNano(0));
        for (RequestListener listener : listeners) {
            listener.onStateChanged(change);
        }
    }

    /** Прогнать заявку по цепочке согласования (паттерн «Цепочка обязанностей»). */
    ApprovalDecision runApproval() {
        decision = approvalChain.handle(new ApprovalRequest(trip, cost));
        return decision;
    }

    /** Оплатить и забронировать поездку (паттерн «Сага»). */
    SagaResult runBooking() {
        lastBooking = booking.run(BookingOrder.of(id, trip, cost));
        return lastBooking;
    }

    /** Итог последней попытки бронирования или {@code null}. */
    public SagaResult lastBooking() {
        return lastBooking;
    }

    /** Кто согласует заявку по результату цепочки. */
    public String approver() {
        return decision == null ? "" : decision.handler();
    }

    // ------------------------------------------------------------ чтение

    public int id() {
        return id;
    }

    public Trip trip() {
        return trip;
    }

    public CostBreakdown cost() {
        return cost;
    }

    public RequestState state() {
        return state;
    }

    public boolean can(RequestAction action) {
        return state.allowedActions().contains(action);
    }
}
