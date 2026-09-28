package ru.tripcost.model.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TripDirectorTest {

    @Test
    void everyPresetBuildsValidTrip() {
        TripDirector director = new TripDirector(new TripBuilder());
        for (TripPreset preset : TripPreset.values()) {
            assertNotNull(director.construct(preset), preset.name());
        }
    }

    @Test
    void businessTripGoesByTrain() {
        Trip trip = new TripDirector(new TripBuilder()).construct(TripPreset.BUSINESS_BY_TRAIN);

        assertEquals(TransportType.TRAIN, trip.getTransport());
        assertEquals("Москва", trip.getDestination());
        assertEquals(3, trip.getNights());
    }
}
