package ru.tripcost.model.transport;

import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.config.Tariff;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;

/**
 * Такси: посадка и цена за километр оплачиваются за машину целиком.
 */
public class Taxi implements Transport {

    private final Tariff tariff = AppConfig.getInstance().getTariff(TransportType.TAXI);
    private final String currency = AppConfig.getInstance().getCurrency();

    @Override
    public TransportType getType() {
        return TransportType.TAXI;
    }

    @Override
    public BigDecimal cost(Trip trip) {
        return tariff.baseFee().multiply(BigDecimal.valueOf(trip.getLegs()))
                .add(tariff.perKm().multiply(BigDecimal.valueOf(trip.getTotalDistanceKm())));
    }

    @Override
    public String costLabel() {
        return "Поездка на такси";
    }

    @Override
    public String describeBaseFee() {
        return Money.plain(tariff.baseFee()) + " " + currency + " за машину";
    }

    @Override
    public String describePerKm() {
        return Money.plain(tariff.perKm()) + " " + currency + "/км за машину";
    }
}
