package ru.tripcost.model.pricing;

import java.math.BigDecimal;

/**
 * Стратегия «время - деньги»: к стоимости поездки добавляется стоимость времени
 * в пути всех пассажиров. Цена часа задаётся в настройках.
 */
public class TimeValueChoice implements TransportChoiceStrategy {

    private final BigDecimal hourValue;

    public TimeValueChoice(BigDecimal hourValue) {
        this.hourValue = hourValue;
    }

    @Override
    public String name() {
        return "Время - деньги (" + hourValue.stripTrailingZeros().toPlainString() + " ₽ за час)";
    }

    @Override
    public double score(ComparisonRow row) {
        double timeCost = row.totalHours() * hourValue.doubleValue() * row.passengers();
        return row.cost().total().doubleValue() + timeCost;
    }

    @Override
    public String explain(ComparisonRow row) {
        return "с учётом времени " + Math.round(score(row)) + " ₽";
    }

    @Override
    public String toString() {
        return name();
    }
}
