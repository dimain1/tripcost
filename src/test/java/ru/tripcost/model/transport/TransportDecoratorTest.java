package ru.tripcost.model.transport;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.CostGroup;
import ru.tripcost.model.domain.CostItem;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripOption;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportDecoratorTest {

    // самолёт, 1000 км туда и обратно, 2 пассажира: билеты 28 000 ₽
    private final Trip trip = Trip.builder().from("А").to("Б").distance(1000)
            .by(TransportType.PLANE).passengers(2).roundTrip(true).build();

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "ожидалось " + expected + ", получено " + actual);
    }

    @Test
    void baggageAddsFeePerPassengerAndLeg() {
        Transport plane = new BaggageDecorator(new Plane());
        assertMoney("30800", plane.cost(trip));   // 28 000 + 700 × 2 × 2
    }

    @Test
    void decoratorsStackInAnyCombination() {
        Transport plane = new InsuranceDecorator(new BaggageDecorator(new Plane()));
        assertMoney("31724", plane.cost(trip));   // (28 000 + 2 800) × 1,03
    }

    @Test
    void discountAndSeasonArePercentOfInnerCost() {
        assertMoney("21000", new DiscountDecorator(new Plane()).cost(trip));     // −25 %
        assertMoney("33600", new PeakSeasonDecorator(new Plane()).cost(trip));   // +20 %
    }

    @Test
    void decoratorKeepsInterfaceOfWrappedObject() {
        Transport plain = new Plane();
        Transport decorated = new InsuranceDecorator(plain);
        assertEquals(plain.getType(), decorated.getType());
        assertEquals(plain.describePerKm(), decorated.describePerKm());
    }

    @Test
    void breakdownListsEveryDecoratorAsLeaf() {
        Transport plane = TransportDecorators.decorate(new Plane(),
                EnumSet.of(TripOption.BAGGAGE, TripOption.INSURANCE));
        CostGroup group = plane.breakdown(trip);

        List<String> names = group.children().stream().map(CostItem::name).toList();
        assertEquals("Билеты", names.get(0));
        assertEquals("Багаж", names.get(1));
        assertTrue(names.get(2).startsWith("Страховка"));
        assertEquals(0, group.amount().compareTo(plane.cost(trip)));
    }

    @Test
    void factoryDecoratesTransportByTripOptions() {
        Trip withOptions = Trip.builder().from("А").to("Б").distance(1000).by(TransportType.PLANE)
                .passengers(2).roundTrip(true).option(TripOption.BAGGAGE).build();
        assertTrue(new PlaneFactory().createFor(withOptions) instanceof BaggageDecorator);
        assertMoney("30800", new PlaneFactory().transportCost(withOptions));
    }
}
