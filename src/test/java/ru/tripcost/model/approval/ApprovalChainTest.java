package ru.tripcost.model.approval;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripDirector;
import ru.tripcost.model.domain.TripPreset;
import ru.tripcost.model.pricing.TripCostCalculator;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalChainTest {

    private final ApprovalHandler chain = ApprovalChain.standard();
    private final TripCostCalculator calculator = new TripCostCalculator();

    private ApprovalDecision approve(Trip trip) {
        return chain.handle(new ApprovalRequest(trip, calculator.calculate(trip)));
    }

    private static Trip preset(TripPreset preset) {
        return new TripDirector(Trip.builder()).construct(preset);
    }

    @Test
    void chainHasFiveLinksInOrder() {
        List<String> names = chain.chainNames();
        assertEquals(5, names.size());
        assertEquals("Политика поездок", names.get(0));
        assertEquals("Финансовый комитет", names.get(4));
    }

    @Test
    void cheapTripIsApprovedAutomatically() {
        ApprovalDecision d = approve(preset(TripPreset.CITY_TAXI));
        assertTrue(d.approved());
        assertTrue(d.handler().startsWith("Автосогласование"));
        assertEquals(2, d.trace().size());   // политика -> автосогласование
    }

    @Test
    void businessTripGoesToManager() {
        ApprovalDecision d = approve(preset(TripPreset.BUSINESS_BY_TRAIN));
        assertTrue(d.approved());
        assertTrue(d.handler().startsWith("Руководитель отдела"));
        assertTrue(d.trace().get(1).contains("превышен лимит"));
    }

    @Test
    void vacationGoesToDirector() {
        ApprovalDecision d = approve(preset(TripPreset.VACATION_BY_PLANE));
        assertTrue(d.handler().startsWith("Директор"));
        assertEquals(4, d.trace().size());
    }

    @Test
    void veryExpensiveTripReachesEndOfChain() {
        Trip trip = Trip.builder().from("Москва").to("Новосибирск").distance(2800)
                .by(TransportType.PLANE).passengers(20).roundTrip(true).build();
        ApprovalDecision d = approve(trip);
        assertTrue(d.approved());
        assertEquals("Финансовый комитет", d.handler());
        assertEquals(5, d.trace().size());
    }

    @Test
    void policyViolationStopsChainAtFirstLink() {
        Trip taxi = Trip.builder().from("Ставрополь").to("Краснодар").distance(280)
                .by(TransportType.TAXI).passengers(1).build();
        ApprovalDecision d = approve(taxi);
        assertFalse(d.approved());
        assertEquals("Политика поездок", d.handler());
        assertEquals(1, d.trace().size());
        assertTrue(d.reason().contains("такси"));
    }

    @Test
    void shortFlightAndExpensiveHotelViolatePolicy() {
        Trip trip = Trip.builder().from("Ставрополь").to("Сочи").distance(300)
                .by(TransportType.PLANE).passengers(1)
                .accommodation(2, new BigDecimal("9000")).build();
        ApprovalDecision d = approve(trip);
        assertFalse(d.approved());
        assertTrue(d.reason().contains("самолёт"));
        assertTrue(d.reason().contains("проживание"));
    }

    @Test
    void chainWithoutFinalLinkRejectsWhenNobodyCanDecide() {
        ApprovalHandler shortChain = new LimitApprovalHandler("Руководитель", new BigDecimal("100"));
        Trip trip = preset(TripPreset.BUSINESS_BY_TRAIN);
        ApprovalDecision d = shortChain.handle(new ApprovalRequest(trip, calculator.calculate(trip)));
        assertFalse(d.approved());
    }

    @Test
    void linksCanBeRearranged() {
        ApprovalHandler head = new LimitApprovalHandler("Директор", new BigDecimal("1000000"));
        head.linkWith(new FinanceCommitteeHandler());
        Trip trip = preset(TripPreset.VACATION_BY_PLANE);
        assertEquals("Директор (до 1 000 000 ₽)",
                head.handle(new ApprovalRequest(trip, calculator.calculate(trip))).handler().replace('\u00a0', ' ').replace('\u202f', ' '));
    }
}
