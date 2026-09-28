package ru.tripcost.model.transport;

import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.config.Tariff;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;

/**
 * Общественный транспорт: каждый пассажир покупает билет.
 * Билет = фиксированная часть + цена за километр, за каждую поездку в одну сторону.
 */
public abstract class TicketTransport implements Transport {

    private final TransportType type;
    private final Tariff tariff;
    private final String currency;

    protected TicketTransport(TransportType type) {
        this.type = type;
        this.tariff = AppConfig.getInstance().getTariff(type);
        this.currency = AppConfig.getInstance().getCurrency();
    }

    @Override
    public TransportType getType() {
        return type;
    }

    @Override
    public BigDecimal cost(Trip trip) {
        BigDecimal ticket = tariff.baseFee().multiply(BigDecimal.valueOf(trip.getLegs()))
                .add(tariff.perKm().multiply(BigDecimal.valueOf(trip.getTotalDistanceKm())));
        return ticket.multiply(BigDecimal.valueOf(trip.getPassengers()));
    }

    @Override
    public String costLabel() {
        return "Билеты";
    }

    @Override
    public String describeBaseFee() {
        return Money.plain(tariff.baseFee()) + " " + currency + " с пассажира";
    }

    @Override
    public String describePerKm() {
        return Money.plain(tariff.perKm()) + " " + currency + "/км с пассажира";
    }
}
