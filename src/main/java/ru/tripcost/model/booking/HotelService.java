package ru.tripcost.model.booking;

/** Сервис бронирования гостиниц. */
public class HotelService extends RemoteService {

    public HotelService() {
        super("Г");
    }

    public String book(BookingOrder order) {
        return register("гостиница, " + order.nights() + " ноч.", "В гостинице нет свободных мест");
    }

    /** Компенсация: отмена брони. */
    public void cancel(String bookingId) {
        unregister(bookingId);
    }
}
