package ru.tripcost.model.pricing;

/** Стратегия: быстрее всего (по времени в пути с дорогой до вокзала или аэропорта). */
public class FastestChoice implements TransportChoiceStrategy {

    @Override
    public String name() {
        return "Быстрее всего";
    }

    @Override
    public double score(ComparisonRow row) {
        return row.totalHours();
    }

    @Override
    public String explain(ComparisonRow row) {
        return "";
    }

    @Override
    public String toString() {
        return name();
    }
}
