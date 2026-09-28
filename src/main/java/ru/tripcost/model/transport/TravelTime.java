package ru.tripcost.model.transport;

import java.util.List;

/**
 * Время в пути в одну сторону с разбивкой по этапам.
 */
public record TravelTime(List<Stage> stages) {

    public TravelTime {
        stages = List.copyOf(stages);
    }

    /** Этап поездки и его продолжительность, ч. */
    public record Stage(String name, double hours) {
    }

    public double hours() {
        return stages.stream().mapToDouble(Stage::hours).sum();
    }

    /** «5 ч 20 мин». */
    public static String format(double hours) {
        long minutes = Math.round(hours * 60);
        long h = minutes / 60;
        long m = minutes % 60;
        return h == 0 ? m + " мин" : (m == 0 ? h + " ч" : h + " ч " + m + " мин");
    }

    @Override
    public String toString() {
        return format(hours());
    }

    /** «дорога до вокзала 30 мин, в пути 5 ч, ...». */
    public String details() {
        return String.join(", ", stages.stream().map(s -> s.name() + " " + format(s.hours())).toList());
    }
}
