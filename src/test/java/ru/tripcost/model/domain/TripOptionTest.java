package ru.tripcost.model.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripOptionTest {

    @Test
    void baggageIsNotAllowedForCar() {
        assertThrows(TripValidationException.class, () -> Trip.builder()
                .from("А").to("Б").distance(10).by(TransportType.CAR).option(TripOption.BAGGAGE).build());
    }

    @Test
    void copyOfBuilderHasIndependentOptions() {
        TripBuilder original = Trip.builder().from("А").to("Б").distance(10).by(TransportType.TRAIN)
                .option(TripOption.INSURANCE);
        TripBuilder copy = original.copy();

        copy.option(TripOption.BAGGAGE);

        Trip trip = original.build();
        assertTrue(trip.getOptions().contains(TripOption.INSURANCE));
        assertFalse(trip.getOptions().contains(TripOption.BAGGAGE));
    }

    @Test
    void dropInapplicableOptionsForNewTransport() {
        Trip trip = Trip.builder().from("А").to("Б").distance(10).by(TransportType.TRAIN)
                .option(TripOption.BAGGAGE).option(TripOption.INSURANCE)
                .by(TransportType.CAR).dropInapplicableOptions().build();

        assertFalse(trip.getOptions().contains(TripOption.BAGGAGE));
        assertTrue(trip.getOptions().contains(TripOption.INSURANCE));
    }
}
