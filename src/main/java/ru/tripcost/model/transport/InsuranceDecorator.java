package ru.tripcost.model.transport;

import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;

/**
 * Конкретный декоратор. Страховка поездки: процент от стоимости проезда со всеми услугами.
 */
public class InsuranceDecorator extends TransportDecorator {

    private final AppConfig config = AppConfig.getInstance();

    public InsuranceDecorator(Transport inner) {
        super(inner);
    }

    @Override
    protected BigDecimal extra(Trip trip, BigDecimal innerCost) {
        return percent(innerCost, config.getInsurancePercent());
    }

    @Override
    protected String label() {
        return "Страховка " + plainPercent(config.getInsurancePercent());
    }

    private static String plainPercent(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString() + "%";
    }
}
