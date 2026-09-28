package ru.tripcost.model.domain;

import java.util.Objects;

/**
 * Город - приспособленец (Flyweight).
 *
 * <p><b>Паттерн «Приспособленец» (Flyweight).</b> Название, регион и федеральный
 * округ - внутреннее состояние, общее для всех маршрутов, где встречается город.
 * Объект неизменяемый и создаётся только фабрикой {@link CityFactory}, которая
 * возвращает один и тот же экземпляр на каждый город. Внешнее состояние
 * (расстояние, транспорт, стоимость) хранится в {@link RouteQuote} и передаётся
 * в методы города параметрами.</p>
 */
public final class City {

    private final String name;
    private final String region;
    private final String federalDistrict;

    /** Создаётся только фабрикой. */
    City(String name, String region, String federalDistrict) {
        this.name = Objects.requireNonNull(name);
        this.region = region;
        this.federalDistrict = federalDistrict;
    }

    public String getName() {
        return name;
    }

    public String getRegion() {
        return region;
    }

    public String getFederalDistrict() {
        return federalDistrict;
    }

    /** Подпись маршрута: внутреннее состояние города + внешнее (другой город и расстояние). */
    public String routeTo(City other, double km) {
        return name + " → " + other.name + ", " + Math.round(km) + " км"
                + (federalDistrict.equals(other.federalDistrict) ? "" : " (другой федеральный округ)");
    }

    @Override
    public String toString() {
        return name;
    }
}
