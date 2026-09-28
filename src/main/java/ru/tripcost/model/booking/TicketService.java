package ru.tripcost.model.booking;

/** Сервис билетов (перевозчик). */
public class TicketService extends RemoteService {

    public TicketService() {
        super("Б");
    }

    public String buy(BookingOrder order) {
        return register("билеты " + order.route() + ", " + order.passengers() + " пасс.",
                "Билетов на выбранные даты нет");
    }

    /** Компенсация: возврат билетов. */
    public void refund(String ticketId) {
        unregister(ticketId);
    }
}
