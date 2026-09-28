package ru.tripcost.model.pricing;

/** Стратегия: дешевле всего. */
public class CheapestChoice implements TransportChoiceStrategy {

    @Override
    public String name() {
        return "Дешевле всего";
    }

    @Override
    public double score(ComparisonRow row) {
        return row.cost().total().doubleValue();
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
