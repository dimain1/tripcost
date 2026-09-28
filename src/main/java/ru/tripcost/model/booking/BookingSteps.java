package ru.tripcost.model.booking;

/**
 * Конкретные шаги саги бронирования.
 */
final class BookingSteps {

    private BookingSteps() {
    }

    static SagaStep buyTickets(TicketService tickets) {
        return new SagaStep() {
            public String name() { return "Покупка билетов"; }
            public String execute(BookingOrder o) { return tickets.buy(o); }
            public void compensate(BookingOrder o, String ref) { tickets.refund(ref); }
        };
    }

    static SagaStep bookHotel(HotelService hotels) {
        return new SagaStep() {
            public String name() { return "Бронирование гостиницы"; }
            public boolean appliesTo(BookingOrder o) { return o.needsHotel(); }
            public String execute(BookingOrder o) { return hotels.book(o); }
            public void compensate(BookingOrder o, String ref) { hotels.cancel(ref); }
        };
    }

    static SagaStep issueInsurance(InsuranceService insurance) {
        return new SagaStep() {
            public String name() { return "Оформление страховки"; }
            public boolean appliesTo(BookingOrder o) { return o.needsInsurance(); }
            public String execute(BookingOrder o) { return insurance.issue(o); }
            public void compensate(BookingOrder o, String ref) { insurance.annul(ref); }
        };
    }

    static SagaStep chargeBudget(BudgetService budget) {
        return new SagaStep() {
            public String name() { return "Списание с бюджета отдела"; }
            public String execute(BookingOrder o) { return budget.charge(o); }
            public void compensate(BookingOrder o, String ref) { budget.refund(ref); }
        };
    }
}
