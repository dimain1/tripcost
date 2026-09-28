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

class TransportChoiceStrategyTest {

    private final TransportComparison comparison = new TransportComparison();
    private final TripBuilder moscow = Trip.builder().from("Ставрополь").to("Москва").distance(1540)
            .by(TransportType.TRAIN).passengers(1).roundTrip(true);

    private TransportType best(TransportChoiceStrategy strategy) {
        comparison.setStrategy(strategy);
        return comparison.best(comparison.compare(moscow)).orElseThrow().transport();
    }

    @Test
    void cheapestChoosesBus() {
        assertEquals(TransportType.BUS, best(new CheapestChoice()));
    }

    @Test
    void fastestChoosesPlane() {
        assertEquals(TransportType.PLANE, best(new FastestChoice()));
    }

    @Test
    void timeValueDependsOnHourPrice() {
        assertEquals(TransportType.BUS, best(new TimeValueChoice(new BigDecimal("50"))));
        assertEquals(TransportType.PLANE, best(new TimeValueChoice(new BigDecimal("1000"))));
    }

    @Test
    void rowsAreSortedByStrategyAndUnavailableGoLast() {
        comparison.setStrategy(new FastestChoice());
        List<ComparisonRow> rows = comparison.compare(moscow.copy().passengers(5));
        ComparisonRow last = rows.get(rows.size() - 1);
        assertFalse(last.isAvailable());                       // такси не везёт 5 пассажиров
        for (int i = 1; i < rows.size() && rows.get(i).isAvailable(); i++) {
            assertTrue(rows.get(i - 1).totalHours() <= rows.get(i).totalHours());
        }
    }

    @Test
    void roundTripDoublesTravelTime() {
        comparison.setStrategy(new CheapestChoice());
        ComparisonRow train = comparison.compare(moscow).stream()
                .filter(r -> r.transport() == TransportType.TRAIN).findFirst().orElseThrow();
        assertEquals(2 * train.travelTime().hours(), train.totalHours(), 1e-9);
    }

    @Test
    void strategiesAreListedForUser() {
        assertEquals(3, TransportComparison.strategies().size());
    }
}
