package ru.tripcost.model.domain;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Поездка — продукт паттерна «Строитель».
 *
 * <p>Объект неизменяемый, у него много параметров, часть из них необязательна.
 * Конструктор закрыт от внешнего кода: получить поездку можно только
 * через {@link TripBuilder}, который проверяет данные перед созданием.</p>
 */
public final class Trip {

    private final String origin;
    private final String destination;
    private final double distanceKm;
    private final TransportType transport;
    private final int passengers;
    private final double fuelConsumption;
    private final boolean roundTrip;
    private final BigDecimal tollCost;
    private final int nights;
    private final BigDecimal pricePerNight;
    private final Set<TripOption> options;

    /** Вызывается только строителем после проверки данных. */
    Trip(TripBuilder builder) {
        this.origin = builder.getOrigin();
        this.destination = builder.getDestination();
        this.distanceKm = builder.getDistanceKm();
        this.transport = builder.getTransport();
        this.passengers = builder.getPassengers();
        this.fuelConsumption = builder.getFuelConsumption();
        this.roundTrip = builder.isRoundTrip();
        this.tollCost = builder.getTollCost();
        this.nights = builder.getNights();
        this.pricePerNight = builder.getPricePerNight();
        Set<TripOption> opts = builder.getOptions();
        this.options = Collections.unmodifiableSet(opts.isEmpty()
                ? EnumSet.noneOf(TripOption.class) : EnumSet.copyOf(opts));
    }

    /** Короткий способ получить новый строитель. */
    /** Строитель с теми же частями, что у этой поездки (например, для восстановления шаблона). */
    public TripBuilder toBuilder() {
        return builder().from(origin).to(destination).distance(distanceKm).by(transport)
                .passengers(passengers).fuelConsumption(fuelConsumption).roundTrip(roundTrip)
                .tolls(tollCost).accommodation(nights, pricePerNight).options(options);
    }

    public static TripBuilder builder() {
        return new TripBuilder();
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    /** Расстояние в одну сторону, км. */
    public double getDistanceKm() {
        return distanceKm;
    }

    public TransportType getTransport() {
        return transport;
    }

    public int getPassengers() {
        return passengers;
    }

    /** Расход топлива, л/100 км (используется только для личного автомобиля). */
    public double getFuelConsumption() {
        return fuelConsumption;
    }

    public boolean isRoundTrip() {
        return roundTrip;
    }

    /** Стоимость платных дорог в одну сторону. */
    public BigDecimal getTollCost() {
        return tollCost;
    }

    public int getNights() {
        return nights;
    }

    public BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    /** Подключённые дополнительные услуги (только чтение). */
    public Set<TripOption> getOptions() {
        return options;
    }

    /** Количество поездок в одну сторону: 1 или 2 (туда и обратно). */
    public int getLegs() {
        return roundTrip ? 2 : 1;
    }

    /** Общий пробег с учётом обратного пути, км. */
    public double getTotalDistanceKm() {
        return distanceKm * getLegs();
    }

    @Override
    public String toString() {
        return origin + " → " + destination
                + (roundTrip ? " и обратно" : "")
                + ", " + transport.getDisplayName().toLowerCase()
                + ", пассажиров: " + passengers;
    }
}
