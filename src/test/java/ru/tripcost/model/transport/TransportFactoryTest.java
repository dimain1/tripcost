package ru.tripcost.model.transport;

import org.junit.jupiter.api.Test;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransportFactoryTest {

    @Test
    void eachFactoryCreatesItsOwnTransport() {
        assertTrue(new CarFactory().createTransport() instanceof PrivateCar);
        assertTrue(new TaxiFactory().createTransport() instanceof Taxi);
        assertTrue(new BusFactory().createTransport() instanceof Bus);
        assertTrue(new TrainFactory().createTransport() instanceof Train);
        assertTrue(new PlaneFactory().createTransport() instanceof Plane);
    }

    @Test
    void registryReturnsFactoryForEveryType() {
        for (TransportType type : TransportType.values()) {
            assertEquals(type, TransportFactories.forType(type).createTransport().getType());
        }
    }

    @Test
    void factoryMethodCreatesNewObjectEachTime() {
        TransportFactory factory = new TrainFactory();
        assertNotSame(factory.createTransport(), factory.createTransport());
    }

    @Test
    void planeTicketsArePerPassenger() {
        // (1500 × 2 + 1000 × 2 × 5,5) × 2 пассажира = 28 000
        Trip trip = Trip.builder().from("А").to("Б").distance(1000)
                .by(TransportType.PLANE).passengers(2).roundTrip(true).build();

        assertEquals(0, new BigDecimal("28000").compareTo(new PlaneFactory().transportCost(trip)));
    }

    @Test
    void factoryRejectsTripOnOtherTransport() {
        Trip trip = Trip.builder().from("А").to("Б").distance(10).by(TransportType.BUS).build();
        assertThrows(IllegalArgumentException.class, () -> new TaxiFactory().transportCost(trip));
    }
}
