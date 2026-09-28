package ru.tripcost.model.transport;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelTimeEstimatorTest {

    private static double hours(TransportType type, double km) {
        return TravelTimeEstimators.forType(type).estimate(km).hours();
    }

    @Test
    void templateKeepsStageOrder() {
        TravelTime plane = TravelTimeEstimators.forType(TransportType.PLANE).estimate(1400);
        List<String> names = plane.stages().stream().map(TravelTime.Stage::name).toList();
        assertEquals(List.of("дорога до места посадки", "ожидание и посадка", "в движении",
                "дорога от места прибытия"), names);
        assertEquals(1.0 + 1.5 + 2.0 + 0.75, plane.hours(), 1e-9);
    }

    @Test
    void unusedHooksAreSkipped() {
        TravelTime taxi = TravelTimeEstimators.forType(TransportType.TAXI).estimate(70);
        assertEquals(2, taxi.stages().size());           // подача + движение
        assertEquals(1.25, taxi.hours(), 1e-9);
    }

    @Test
    void carDriverRestsEveryTwoHours() {
        TravelTime car = TravelTimeEstimators.forType(TransportType.CAR).estimate(400); // 5 ч за рулём
        assertEquals(5 + 2 * 0.25, car.hours(), 1e-9);
        assertTrue(car.details().contains("отдых водителя 30 мин"));
    }

    @Test
    void planeLosesOnShortDistanceAndWinsOnLong() {
        assertTrue(hours(TransportType.PLANE, 150) > hours(TransportType.CAR, 150));
        assertTrue(hours(TransportType.PLANE, 1500) < hours(TransportType.TRAIN, 1500));
    }

    @Test
    void formatsHoursAndMinutes() {
        assertEquals("5 ч 20 мин", TravelTime.format(5 + 1 / 3.0));
        assertEquals("45 мин", TravelTime.format(0.75));
        assertEquals("2 ч", TravelTime.format(2));
    }
}
