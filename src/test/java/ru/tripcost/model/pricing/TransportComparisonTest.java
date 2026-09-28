package ru.tripcost.model.pricing;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripBuilder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportComparisonTest {

    private final TransportComparison comparison = new TransportComparison();

    @Test
    void comparesAllTransportTypes() {
        TripBuilder base = Trip.builder().from("А").to("Б").distance(100).by(TransportType.CAR).passengers(2);

        List<ComparisonRow> rows = comparison.compare(base);

        assertEquals(TransportType.values().length, rows.size());
        assertTrue(rows.stream().allMatch(ComparisonRow::isAvailable));
    }

    @Test
    void taxiIsUnavailableForFivePassengers() {
        TripBuilder base = Trip.builder().from("А").to("Б").distance(100).by(TransportType.CAR).passengers(5);

        ComparisonRow taxi = comparison.compare(base).stream()
                .filter(r -> r.transport() == TransportType.TAXI).findFirst().orElseThrow();

        assertFalse(taxi.isAvailable());
    }

    @Test
    void prototypeIsNotChanged() {
        TripBuilder base = Trip.builder().from("А").to("Б").distance(100).by(TransportType.CAR)
                .tolls(new BigDecimal("200"));

        comparison.compare(base);

        Trip trip = base.build();
        assertEquals(TransportType.CAR, trip.getTransport());
        assertEquals(0, new BigDecimal("200").compareTo(trip.getTollCost()));
    }
}
