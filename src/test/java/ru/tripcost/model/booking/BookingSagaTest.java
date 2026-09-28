package ru.tripcost.model.booking;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.approval.ApprovalChain;
import ru.tripcost.model.booking.SagaLogEntry.Kind;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripDirector;
import ru.tripcost.model.domain.TripPreset;
import ru.tripcost.model.pricing.TripCostCalculator;
import ru.tripcost.model.request.NotificationCenter;
import ru.tripcost.model.request.RequestStates;
import ru.tripcost.model.request.TripRequest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingSagaTest {

    private final TravelServices services = TravelServices.create(new BigDecimal("150000"));
    private final BookingSaga saga = BookingSaga.standard(services);

    private static Trip preset(TripPreset preset) {
        return new TripDirector(Trip.builder()).construct(preset);
    }

    private static BookingOrder order(TripPreset preset) {
        Trip trip = preset(preset);
        return BookingOrder.of(1, trip, new TripCostCalculator().calculate(trip));
    }

    private static List<Kind> kinds(SagaResult r) {
        return r.log().stream().map(SagaLogEntry::kind).toList();
    }

    @Test
    void allStepsSucceed() {
        BookingOrder order = order(TripPreset.VACATION_BY_PLANE);   // гостиница и страховка
        SagaResult r = saga.run(order);

        assertTrue(r.success());
        assertEquals(List.of(Kind.DONE, Kind.DONE, Kind.DONE, Kind.DONE), kinds(r));
        assertEquals(1, services.tickets().active().size());
        assertEquals(1, services.hotels().active().size());
        assertEquals(1, services.insurance().active().size());
        assertEquals(0, new BigDecimal("150000").subtract(order.total()).compareTo(services.budget().balance()));
    }

    @Test
    void unnecessaryStepsAreSkipped() {
        SagaResult r = saga.run(order(TripPreset.CITY_TAXI));        // без ночёвок и страховки
        assertTrue(r.success());
        assertEquals(List.of(Kind.DONE, Kind.SKIPPED, Kind.SKIPPED, Kind.DONE), kinds(r));
    }

    @Test
    void hotelFailureRefundsTickets() {
        services.hotels().setFailing(true);
        SagaResult r = saga.run(order(TripPreset.BUSINESS_BY_TRAIN));

        assertFalse(r.success());
        assertEquals(List.of(Kind.DONE, Kind.FAILED, Kind.COMPENSATED), kinds(r));
        assertTrue(services.tickets().active().isEmpty());
        assertEquals(0, new BigDecimal("150000").compareTo(services.budget().balance()));
        assertTrue(r.failure().startsWith("Бронирование гостиницы"));
    }

    @Test
    void budgetFailureCompensatesInReverseOrder() {
        BookingOrder vacation = order(TripPreset.VACATION_BY_PLANE);     // 94 118 ₽
        assertTrue(saga.run(vacation).success());                        // осталось 55 882 ₽
        SagaResult second = saga.run(vacation);                          // денег не хватает

        assertFalse(second.success());
        List<SagaLogEntry> log = second.log();
        assertEquals(Kind.FAILED, log.get(3).kind());
        assertEquals("Оформление страховки", log.get(4).step());         // сначала последний шаг
        assertEquals("Бронирование гостиницы", log.get(5).step());
        assertEquals("Покупка билетов", log.get(6).step());
        assertEquals(1, services.tickets().active().size());             // остались только первые брони
        assertEquals(1, services.hotels().active().size());
        assertEquals(1, services.insurance().active().size());
    }

    @Test
    void payingRequestRunsSaga() {
        Trip trip = preset(TripPreset.BUSINESS_BY_TRAIN);
        TripRequest request = new TripRequest(7, trip, new TripCostCalculator().calculate(trip),
                ApprovalChain.standard(), saga);
        NotificationCenter notifications = new NotificationCenter();
        request.addListener(notifications);
        request.submit();
        request.approve();

        services.tickets().setFailing(true);
        request.pay();
        assertSame(RequestStates.APPROVED, request.state());           // бронирование отменено
        assertFalse(request.lastBooking().success());
        assertTrue(notifications.messages().stream().anyMatch(m -> m.contains("не выполнено")));

        services.tickets().setFailing(false);
        request.pay();
        assertSame(RequestStates.PAID, request.state());
        assertTrue(request.lastBooking().success());
        assertEquals(3, request.lastBooking().references().size());    // билеты, гостиница, бюджет
    }
}
