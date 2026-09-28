package ru.tripcost.model.transport;

import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Личный автомобиль: платят за топливо, стоимость не зависит от числа пассажиров.
 */
public class PrivateCar implements Transport {

    private final AppConfig config = AppConfig.getInstance();

    @Override
    public TransportType getType() {
        return TransportType.CAR;
    }

    @Override
    public BigDecimal cost(Trip trip) {
        // литры = км × расход / 100; стоимость = литры × цена литра
        BigDecimal liters = BigDecimal.valueOf(trip.getTotalDistanceKm())
                .multiply(BigDecimal.valueOf(trip.getFuelConsumption()))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return liters.multiply(config.getFuelPrice());
    }

    @Override
    public String costLabel() {
        return "Топливо";
    }

    @Override
    public String describeBaseFee() {
        return "—";
    }

    @Override
    public String describePerKm() {
        return "топливо " + Money.plain(config.getFuelPrice()) + " " + config.getCurrency()
                + "/л, расход по умолчанию " + config.getDefaultFuelConsumption() + " л/100 км";
    }
}
