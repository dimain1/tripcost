package ru.tripcost.model.approval;

import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.pricing.CostBreakdown;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Запрос на согласование поездки: сама поездка и её рассчитанная стоимость.
 */
public record ApprovalRequest(Trip trip, CostBreakdown cost) {

    public ApprovalRequest {
        Objects.requireNonNull(trip);
        Objects.requireNonNull(cost);
    }

    public BigDecimal total() {
        return cost.total();
    }
}
