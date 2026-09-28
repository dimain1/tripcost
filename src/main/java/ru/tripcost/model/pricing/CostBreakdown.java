package ru.tripcost.model.pricing;

import ru.tripcost.model.domain.CostGroup;

import java.math.BigDecimal;

/**
 * Результат расчёта: стоимость по статьям, итог и доля одного пассажира.
 * {@code items} - те же расходы в виде дерева статей (паттерн «Компоновщик»).
 */
public record CostBreakdown(
        BigDecimal transport,
        BigDecimal tolls,
        BigDecimal accommodation,
        BigDecimal total,
        BigDecimal perPassenger,
        String currency,
        CostGroup items) {
}
