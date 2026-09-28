package ru.tripcost.model.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripTemplateRegistryTest {

    @Test
    void containsDirectorPresets() {
        TripTemplateRegistry registry = TripTemplateRegistry.withPresets();
        for (TripPreset preset : TripPreset.values()) {
            assertTrue(registry.contains(preset.getDisplayName()));
        }
    }

    @Test
    void createReturnsNewCopyEveryTime() {
        TripTemplateRegistry registry = TripTemplateRegistry.withPresets();
        String name = TripPreset.CITY_TAXI.getDisplayName();
        assertNotSame(registry.create(name), registry.create(name));
    }

    @Test
    void changingCopyDoesNotChangeTemplate() {
        TripTemplateRegistry registry = TripTemplateRegistry.withPresets();
        String name = TripPreset.WEEKEND_BY_CAR.getDisplayName();

        registry.create(name).passengers(5).to("Пятигорск");

        Trip trip = registry.create(name).build();
        assertEquals(3, trip.getPassengers());
        assertEquals("Кисловодск", trip.getDestination());
    }

    @Test
    void registerStoresCopyOfBuilder() {
        TripTemplateRegistry registry = new TripTemplateRegistry();
        TripBuilder builder = Trip.builder().from("А").to("Б").distance(10).by(TransportType.BUS);
        registry.register("Мой", builder);

        builder.to("В");   // меняем исходный строитель после регистрации

        assertEquals("Б", registry.create("Мой").build().getDestination());
    }

    @Test
    void unknownTemplateIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TripTemplateRegistry().create("Нет такого"));
    }
}
