package ru.tripcost.model.transport;

import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;

/**
 * Конкретный декоратор. Наценка высокого сезона: процент от стоимости проезда.
 */
public class PeakSeasonDecorator extends TransportDecorator {

    private final AppConfig config = AppConfig.getInstance();

    public PeakSeasonDecorator(Transport inner) {
        super(inner);
    }

    @Override
    protected BigDecimal extra(Trip trip, BigDecimal innerCost) {
        return percent(innerCost, config.getPeakSeasonPercent());
    }

    @Override
    protected String label() {
        return "Наценка высокого сезона +" + plainPercent(config.getPeakSeasonPercent());
    }

    private static String plainPercent(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString() + "%";
    }
}
