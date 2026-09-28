package ru.tripcost.model.transport;

import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;

/**
 * Конкретный декоратор. Льготный тариф (студент, пенсионер): скидка на билеты.
 */
public class DiscountDecorator extends TransportDecorator {

    private final AppConfig config = AppConfig.getInstance();

    public DiscountDecorator(Transport inner) {
        super(inner);
    }

    @Override
    protected BigDecimal extra(Trip trip, BigDecimal innerCost) {
        return percent(innerCost, config.getDiscountPercent()).negate();
    }

    @Override
    protected String label() {
        return "Льготная скидка −" + plainPercent(config.getDiscountPercent());
    }

    private static String plainPercent(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString() + "%";
    }
}
