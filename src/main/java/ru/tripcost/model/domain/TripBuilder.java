package ru.tripcost.model.domain;

import ru.tripcost.model.config.AppConfig;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * Строитель поездки.
 *
 * <p><b>Паттерн «Строитель» (Builder).</b> Поездка собирается по шагам
 * через «текучий» интерфейс: каждый метод задаёт одну часть и возвращает
 * сам строитель. Необязательные параметры получают значения по умолчанию.
 * Метод {@link #build()} проверяет согласованность всех частей и только
 * потом создаёт неизменяемый объект {@link Trip}.</p>
 *
 * <p><b>Паттерн «Прототип» (Prototype).</b> Настроенный строитель можно
 * скопировать методом {@link #copy()} и получить независимую заготовку
 * поездки. Так работают шаблоны поездок ({@link TripTemplateRegistry})
 * и сравнение видов транспорта: копия меняется, оригинал — нет.</p>
 */
public class TripBuilder implements Prototype<TripBuilder> {

    private String origin;
    private String destination;
    private double distanceKm;
    private TransportType transport;
    private int passengers;
    private double fuelConsumption;
    private boolean roundTrip;
    private BigDecimal tollCost;
    private int nights;
    private BigDecimal pricePerNight;
    private EnumSet<TripOption> options;

    public TripBuilder() {
        setDefaults();
    }

    /** Конструктор копирования: переносит все части поездки из прототипа. */
    private TripBuilder(TripBuilder source) {
        this.origin = source.origin;
        this.destination = source.destination;
        this.distanceKm = source.distanceKm;
        this.transport = source.transport;
        this.passengers = source.passengers;
        this.fuelConsumption = source.fuelConsumption;
        this.roundTrip = source.roundTrip;
        this.tollCost = source.tollCost;          // BigDecimal неизменяем — общий объект безопасен
        this.nights = source.nights;
        this.pricePerNight = source.pricePerNight;
        this.options = EnumSet.copyOf(source.options);   // изменяемая коллекция - копируем глубоко
    }

    /** Возвращает независимую копию строителя со всеми заданными частями. */
    @Override
    public TripBuilder copy() {
        return new TripBuilder(this);
    }

    /** Сбрасывает строитель к значениям по умолчанию, чтобы собрать новую поездку. */
    public TripBuilder reset() {
        setDefaults();
        return this;
    }

    private void setDefaults() {
        origin = null;
        destination = null;
        distanceKm = 0;
        transport = null;
        passengers = 1;
        fuelConsumption = AppConfig.getInstance().getDefaultFuelConsumption();
        roundTrip = false;
        tollCost = BigDecimal.ZERO;
        nights = 0;
        pricePerNight = BigDecimal.ZERO;
        options = EnumSet.noneOf(TripOption.class);
    }

    public TripBuilder from(String origin) {
        this.origin = origin;
        return this;
    }

    public TripBuilder to(String destination) {
        this.destination = destination;
        return this;
    }

    /** Расстояние в одну сторону, км. */
    public TripBuilder distance(double km) {
        this.distanceKm = km;
        return this;
    }

    public TripBuilder by(TransportType transport) {
        this.transport = transport;
        return this;
    }

    public TripBuilder passengers(int count) {
        this.passengers = count;
        return this;
    }

    /** Расход топлива, л/100 км. */
    public TripBuilder fuelConsumption(double litersPer100Km) {
        this.fuelConsumption = litersPer100Km;
        return this;
    }

    public TripBuilder roundTrip(boolean roundTrip) {
        this.roundTrip = roundTrip;
        return this;
    }

    /** Платные дороги в одну сторону. */
    public TripBuilder tolls(BigDecimal cost) {
        this.tollCost = cost == null ? BigDecimal.ZERO : cost;
        return this;
    }

    /** Проживание: число ночей и цена за ночь (за номер). */
    public TripBuilder accommodation(int nights, BigDecimal pricePerNight) {
        this.nights = nights;
        this.pricePerNight = pricePerNight == null ? BigDecimal.ZERO : pricePerNight;
        return this;
    }

    /** Подключает дополнительную услугу. */
    public TripBuilder option(TripOption option) {
        this.options.add(option);
        return this;
    }

    /** Заменяет набор дополнительных услуг. */
    public TripBuilder options(Collection<TripOption> options) {
        this.options = options.isEmpty() ? EnumSet.noneOf(TripOption.class) : EnumSet.copyOf(options);
        return this;
    }

    /** Убирает услуги, недоступные для выбранного вида транспорта. */
    public TripBuilder dropInapplicableOptions() {
        options.removeIf(o -> !o.appliesTo(transport));
        return this;
    }

    /**
     * Проверяет данные и создаёт поездку.
     *
     * @throws TripValidationException если данные неполны или противоречивы
     */
    public Trip build() {
        List<String> errors = new ArrayList<>();

        if (isBlank(origin)) {
            errors.add("Не указан пункт отправления");
        }
        if (isBlank(destination)) {
            errors.add("Не указан пункт назначения");
        }
        if (!isBlank(origin) && origin.trim().equalsIgnoreCase(destination == null ? "" : destination.trim())) {
            errors.add("Пункты отправления и назначения совпадают");
        }
        if (distanceKm <= 0) {
            errors.add("Расстояние должно быть больше нуля");
        }
        if (transport == null) {
            errors.add("Не выбран вид транспорта");
        } else {
            if (passengers < 1 || passengers > transport.getMaxPassengers()) {
                errors.add("Число пассажиров для вида транспорта «" + transport
                        + "»: от 1 до " + transport.getMaxPassengers());
            }
            if (transport.usesFuel() && (fuelConsumption <= 0 || fuelConsumption > 50)) {
                errors.add("Расход топлива должен быть от 0 до 50 л/100 км");
            }
            if (!transport.allowsTolls() && tollCost.signum() > 0) {
                errors.add("Платные дороги учитываются только для автомобиля и такси");
            }
            for (TripOption option : options) {
                if (!option.appliesTo(transport)) {
                    errors.add("Услуга «" + option + "» недоступна для вида транспорта «" + transport + "»");
                }
            }
        }
        if (tollCost.signum() < 0) {
            errors.add("Стоимость платных дорог не может быть отрицательной");
        }
        if (nights < 0) {
            errors.add("Число ночей не может быть отрицательным");
        }
        if (pricePerNight.signum() < 0) {
            errors.add("Цена за ночь не может быть отрицательной");
        }
        if (nights > 0 && pricePerNight.signum() == 0) {
            errors.add("Указаны ночи проживания, но не указана цена за ночь");
        }

        if (!errors.isEmpty()) {
            throw new TripValidationException(errors);
        }
        origin = origin.trim();
        destination = destination.trim();
        return new Trip(this);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    // Геттеры доступны только внутри пакета — их использует конструктор Trip.

    String getOrigin() {
        return origin;
    }

    String getDestination() {
        return destination;
    }

    double getDistanceKm() {
        return distanceKm;
    }

    TransportType getTransport() {
        return transport;
    }

    int getPassengers() {
        return passengers;
    }

    double getFuelConsumption() {
        return fuelConsumption;
    }

    boolean isRoundTrip() {
        return roundTrip;
    }

    BigDecimal getTollCost() {
        return tollCost;
    }

    int getNights() {
        return nights;
    }

    BigDecimal getPricePerNight() {
        return pricePerNight;
    }

    EnumSet<TripOption> getOptions() {
        return options;
    }
}
