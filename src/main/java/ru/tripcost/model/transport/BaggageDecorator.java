package ru.tripcost.model.transport;

import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;

/**
 * Конкретный декоратор. Багаж: фиксированная плата за каждого пассажира
 * за каждую поездку в одну сторону.
 */
public class BaggageDecorator extends TransportDecorator {

    private final AppConfig config = AppConfig.getInstance();

    public BaggageDecorator(Transport inner) {
        super(inner);
    }

    @Override
    protected BigDecimal extra(Trip trip, BigDecimal innerCost) {
        return config.getBaggageFee()
                .multiply(BigDecimal.valueOf(trip.getPassengers()))
                .multiply(BigDecimal.valueOf(trip.getLegs()));
    }

    @Override
    protected String label() {
        return "Багаж";
    }
}
