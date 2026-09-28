package ru.tripcost.model.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class TripBuilderPrototypeTest {

    private TripBuilder prototype() {
        return Trip.builder().from("Ставрополь").to("Москва").distance(1540)
                .by(TransportType.TRAIN).passengers(2).roundTrip(true)
                .accommodation(3, new BigDecimal("5500"));
    }

    @Test
    void copyBuildsSameTrip() {
        TripBuilder original = prototype();
        Trip a = original.build();
        Trip b = original.copy().build();

        assertEquals(a.getDestination(), b.getDestination());
        assertEquals(a.getTransport(), b.getTransport());
        assertEquals(a.getPassengers(), b.getPassengers());
        assertEquals(a.getNights(), b.getNights());
        assertEquals(a.isRoundTrip(), b.isRoundTrip());
    }

    @Test
    void copyIsIndependentFromOriginal() {
        TripBuilder original = prototype();
        TripBuilder copy = original.copy();
        assertNotSame(original, copy);

        copy.by(TransportType.PLANE).passengers(4).to("Сочи");

        Trip trip = original.build();
        assertEquals(TransportType.TRAIN, trip.getTransport());
        assertEquals(2, trip.getPassengers());
        assertEquals("Москва", trip.getDestination());
    }
}
