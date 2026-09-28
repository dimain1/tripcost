package ru.tripcost.model.transport;

import java.util.ArrayList;
import java.util.List;

/**
 * Оценка времени в пути.
 *
 * <p><b>Паттерн «Шаблонный метод» (Template Method).</b> Метод {@link #estimate(double)}
 * задаёт общий порядок этапов любой поездки: дорога до места посадки, ожидание
 * и посадка, движение, остановки в пути, дорога от места прибытия. Подклассы
 * задают скорость (обязательный шаг) и переопределяют только нужные им этапы -
 * «перехватчики» (hooks), которые по умолчанию равны нулю.</p>
 */
public abstract class TravelTimeEstimator {

    /** Шаблонный метод: порядок этапов фиксирован и в подклассах не меняется. */
    public final TravelTime estimate(double distanceKm) {
        List<TravelTime.Stage> stages = new ArrayList<>();
        add(stages, "дорога до места посадки", accessHours());
        add(stages, "ожидание и посадка", boardingHours());
        double moving = distanceKm / speedKmh();
        add(stages, "в движении", moving);
        add(stages, stopsName(), stopsHours(moving));
        add(stages, "дорога от места прибытия", egressHours());
        return new TravelTime(stages);
    }

    /** Средняя скорость движения, км/ч - обязательный шаг. */
    protected abstract double speedKmh();

    /** Перехватчик: дорога до вокзала, автостанции или аэропорта. */
    protected double accessHours() {
        return 0;
    }

    /** Перехватчик: ожидание, регистрация и посадка. */
    protected double boardingHours() {
        return 0;
    }

    /** Перехватчик: остановки в пути в зависимости от времени движения. */
    protected double stopsHours(double movingHours) {
        return 0;
    }

    protected String stopsName() {
        return "остановки";
    }

    /** Перехватчик: дорога от места прибытия. */
    protected double egressHours() {
        return 0;
    }

    private static void add(List<TravelTime.Stage> stages, String name, double hours) {
        if (hours > 0) {
            stages.add(new TravelTime.Stage(name, hours));
        }
    }
}
