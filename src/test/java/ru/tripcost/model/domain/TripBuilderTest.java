package ru.tripcost.model.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripBuilderTest {

    @Test
    void buildsTripStepByStep() {
        Trip trip = Trip.builder()
                .from("Ставрополь")
                .to("Кисловодск")
                .distance(210)
                .by(TransportType.CAR)
                .passengers(3)
                .fuelConsumption(7.5)
                .roundTrip(true)
                .tolls(new BigDecimal("100"))
                .accommodation(2, new BigDecimal("4000"))
                .build();

        assertEquals("Ставрополь", trip.getOrigin());
        assertEquals("Кисловодск", trip.getDestination());
        assertEquals(420, trip.getTotalDistanceKm());
        assertEquals(3, trip.getPassengers());
        assertEquals(7.5, trip.getFuelConsumption());
        assertEquals(2, trip.getLegs());
        assertEquals(2, trip.getNights());
    }

    @Test
    void optionalPartsHaveDefaults() {
        Trip trip = Trip.builder().from("А").to("Б").distance(10).by(TransportType.BUS).build();

        assertEquals(1, trip.getPassengers());
        assertFalse(trip.isRoundTrip());
        assertEquals(0, trip.getNights());
        assertEquals(0, trip.getTollCost().signum());
    }

    @Test
    void collectsAllErrors() {
        TripValidationException e = assertThrows(TripValidationException.class,
                () -> Trip.builder().distance(-5).build());

        assertTrue(e.getErrors().contains("Не указан пункт отправления"));
        assertTrue(e.getErrors().contains("Не указан пункт назначения"));
        assertTrue(e.getErrors().contains("Расстояние должно быть больше нуля"));
        assertTrue(e.getErrors().contains("Не выбран вид транспорта"));
    }

    @Test
    void rejectsTooManyPassengers() {
        assertThrows(TripValidationException.class, () -> Trip.builder()
                .from("А").to("Б").distance(10).by(TransportType.TAXI).passengers(5).build());
    }

    @Test
    void rejectsTollsForTrain() {
        assertThrows(TripValidationException.class, () -> Trip.builder()
                .from("А").to("Б").distance(10).by(TransportType.TRAIN).tolls(new BigDecimal("50")).build());
    }

    @Test
    void resetClearsPreviousTrip() {
        TripBuilder builder = new TripBuilder();
        builder.from("А").to("Б").distance(10).by(TransportType.CAR).passengers(4).build();

        builder.reset();

        assertThrows(TripValidationException.class, builder::build);
    }
}
