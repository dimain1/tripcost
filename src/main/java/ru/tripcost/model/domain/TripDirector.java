package ru.tripcost.model.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Распорядитель (Director) паттерна «Строитель».
 *
 * <p>Знает, в каком порядке и с какими значениями вызывать шаги строителя,
 * чтобы получить типовую поездку. Сам объект {@link Trip} создаёт строитель.</p>
 */
public class TripDirector {

    private final TripBuilder builder;

    public TripDirector(TripBuilder builder) {
        this.builder = Objects.requireNonNull(builder);
    }

    /** Собирает поездку по выбранному шаблону. */
    public Trip construct(TripPreset preset) {
        return configure(preset).build();
    }

    /** Настраивает строитель по шаблону, не создавая поездку. */
    public TripBuilder configure(TripPreset preset) {
        builder.reset();
        switch (preset) {
            case WEEKEND_BY_CAR -> weekendByCar();
            case BUSINESS_BY_TRAIN -> businessByTrain();
            case VACATION_BY_PLANE -> vacationByPlane();
            case CITY_TAXI -> cityTaxi();
        }
        return builder;
    }

    private void weekendByCar() {
        builder.from("Ставрополь")
                .to("Кисловодск")
                .distance(210)
                .by(TransportType.CAR)
                .passengers(3)
                .fuelConsumption(8.0)
                .roundTrip(true)
                .accommodation(1, new BigDecimal("4500"));
    }

    private void businessByTrain() {
        builder.from("Ставрополь")
                .to("Москва")
                .distance(1540)
                .by(TransportType.TRAIN)
                .passengers(1)
                .roundTrip(true)
                .accommodation(3, new BigDecimal("5500"));
    }

    private void vacationByPlane() {
        builder.from("Минеральные Воды")
                .to("Санкт-Петербург")
                .distance(1900)
                .by(TransportType.PLANE)
                .passengers(2)
                .roundTrip(true)
                .option(TripOption.BAGGAGE)
                .option(TripOption.INSURANCE)
                .accommodation(7, new BigDecimal("6000"));
    }

    private void cityTaxi() {
        builder.from("Ставрополь, ж/д вокзал")
                .to("Аэропорт Ставрополь")
                .distance(16)
                .by(TransportType.TAXI)
                .passengers(2);
    }
}
