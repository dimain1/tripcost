package ru.tripcost.model.pricing;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripOption;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TripCostCalculatorTest {

    private final TripCostCalculator calculator = new TripCostCalculator();

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "ожидалось " + expected + ", получено " + actual);
    }

    @Test
    void carCostIsFuelPlusTollsPlusHotel() {
        // 200 км туда-обратно = 400 км; 400 × 8 / 100 = 32 л; 32 × 62,50 = 2000
        Trip trip = Trip.builder().from("А").to("Б").distance(200).by(TransportType.CAR)
                .passengers(2).fuelConsumption(8).roundTrip(true)
                .tolls(new BigDecimal("150"))
                .accommodation(1, new BigDecimal("3000"))
                .build();

        CostBreakdown cost = calculator.calculate(trip);

        assertMoney("2000", cost.transport());
        assertMoney("300", cost.tolls());
        assertMoney("3000", cost.accommodation());
        assertMoney("5300", cost.total());
        assertMoney("2650", cost.perPassenger());
    }

    @Test
    void trainTicketsArePerPassenger() {
        // (300 + 100 × 3,8) × 2 пассажира = 1360
        Trip trip = Trip.builder().from("А").to("Б").distance(100).by(TransportType.TRAIN)
                .passengers(2).build();

        assertMoney("1360", calculator.calculate(trip).transport());
    }

    @Test
    void taxiIsPaidPerCar() {
        // 150 + 10 × 28 = 430, не зависит от числа пассажиров
        Trip trip = Trip.builder().from("А").to("Б").distance(10).by(TransportType.TAXI)
                .passengers(3).build();

        CostBreakdown cost = calculator.calculate(trip);
        assertMoney("430", cost.transport());
        assertMoney("143.33", cost.perPassenger());
    }

    @Test
    void costTreeMatchesTotal() {
        Trip trip = Trip.builder().from("А").to("Б").distance(1000).by(TransportType.PLANE)
                .passengers(2).roundTrip(true).option(TripOption.BAGGAGE)
                .accommodation(2, new BigDecimal("5000")).build();

        CostBreakdown cost = calculator.calculate(trip);

        // 28 000 + 2 800 багаж + 10 000 проживание
        assertMoney("40800", cost.total());
        assertMoney("40800", cost.items().amount());
        assertMoney("30800", cost.transport());
        assertEquals(2, cost.items().children().size());
    }
}
