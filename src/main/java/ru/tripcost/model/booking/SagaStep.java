package ru.tripcost.model.booking;

/**
 * Шаг саги: локальная операция одного сервиса и компенсирующая её операция.
 */
public interface SagaStep {

    String name();

    /** Нужен ли шаг для этого заказа (например, гостиница - только при ночёвках). */
    default boolean appliesTo(BookingOrder order) {
        return true;
    }

    /**
     * Выполнить операцию.
     *
     * @return номер операции в сервисе - по нему выполняется компенсация
     * @throws ServiceException если сервис отказал
     */
    String execute(BookingOrder order);

    /** Отменить результат {@link #execute}: вернуть билеты, снять бронь, вернуть деньги. */
    void compensate(BookingOrder order, String reference);
}
