package ru.tripcost.model.request;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.approval.ApprovalChain;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripDirector;
import ru.tripcost.model.domain.TripPreset;
import ru.tripcost.model.pricing.TripCostCalculator;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripRequestStateTest {

    private static TripRequest request(Trip trip) {
        return new TripRequest(1, trip, new TripCostCalculator().calculate(trip), ApprovalChain.standard());
    }

    private static TripRequest request(TripPreset preset) {
        return request(new TripDirector(Trip.builder()).construct(preset));
    }

    @Test
    void newRequestIsDraft() {
        TripRequest r = request(TripPreset.BUSINESS_BY_TRAIN);
        assertSame(RequestStates.DRAFT, r.state());
        assertTrue(r.can(RequestAction.SUBMIT));
        assertFalse(r.can(RequestAction.PAY));
    }

    @Test
    void fullLifecycleThroughManager() {
        TripRequest r = request(TripPreset.BUSINESS_BY_TRAIN);
        r.submit();
        assertSame(RequestStates.PENDING, r.state());
        assertTrue(r.approver().startsWith("Руководитель отдела"));
        r.approve();
        assertSame(RequestStates.APPROVED, r.state());
        r.pay();
        assertSame(RequestStates.PAID, r.state());
        r.complete();
        assertSame(RequestStates.COMPLETED, r.state());
        assertTrue(r.state().isFinal());
    }

    @Test
    void cheapTripIsApprovedRightAfterSubmit() {
        TripRequest r = request(TripPreset.CITY_TAXI);
        r.submit();
        assertSame(RequestStates.APPROVED, r.state());
    }

    @Test
    void policyViolationRejectsAndRequestCanBeRevised() {
        TripRequest r = request(Trip.builder().from("Ставрополь").to("Краснодар").distance(280)
                .by(TransportType.TAXI).passengers(1).build());
        r.submit();
        assertSame(RequestStates.REJECTED, r.state());
        r.revise();
        assertSame(RequestStates.DRAFT, r.state());
    }

    @Test
    void forbiddenActionThrowsAndKeepsState() {
        TripRequest r = request(TripPreset.BUSINESS_BY_TRAIN);
        IllegalStateException e = assertThrows(IllegalStateException.class, r::pay);
        assertTrue(e.getMessage().contains("Черновик"));
        assertSame(RequestStates.DRAFT, r.state());
        r.cancel();
        assertThrows(IllegalStateException.class, r::submit);   // из конечного состояния хода нет
    }

    @Test
    void rejectWithReason() {
        TripRequest r = request(TripPreset.VACATION_BY_PLANE);
        r.submit();
        r.reject("Нет бюджета");
        assertSame(RequestStates.REJECTED, r.state());
    }

    @Test
    void allObserversReceiveEveryTransition() {
        TripRequest r = request(TripPreset.BUSINESS_BY_TRAIN);
        RequestJournal journal = new RequestJournal();
        NotificationCenter notifications = new NotificationCenter();
        List<String> custom = new ArrayList<>();
        r.addListener(journal);
        r.addListener(notifications);
        r.addListener(change -> custom.add(change.to().name()));

        r.submit();
        r.approve();
        r.pay();

        assertEquals(3, journal.of(r).size());
        assertSame(RequestStates.DRAFT, journal.entries().get(0).from());
        assertEquals(List.of("На согласовании", "Согласована", "Оплачена"), custom);
        assertTrue(notifications.messages().stream().anyMatch(m -> m.startsWith("Руководитель отдела")));
        assertTrue(notifications.messages().stream().anyMatch(m -> m.startsWith("Бухгалтерии: оплатите")));
    }

    @Test
    void removedObserverIsNotNotified() {
        TripRequest r = request(TripPreset.CITY_TAXI);
        List<String> seen = new ArrayList<>();
        RequestListener listener = change -> seen.add(change.to().name());
        r.addListener(listener);
        r.removeListener(listener);
        r.submit();
        assertTrue(seen.isEmpty());
    }

    @Test
    void failedActionDoesNotNotify() {
        TripRequest r = request(TripPreset.CITY_TAXI);
        RequestJournal journal = new RequestJournal();
        r.addListener(journal);
        assertThrows(IllegalStateException.class, r::complete);
        assertTrue(journal.entries().isEmpty());
    }
}
