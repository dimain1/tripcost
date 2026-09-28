package ru.tripcost.model.journey;

import ru.tripcost.model.domain.TransportType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Расчёт маршрута целиком.
 *
 * @param legs           расчёты участков по порядку
 * @param total          сумма по рассчитанным участкам
 * @param perPassenger   на одного пассажира
 * @param kmByTransport  километры по видам транспорта (только используемые виды)
 */
public record JourneyQuote(List<LegQuote> legs, BigDecimal total, BigDecimal perPassenger,
                           Map<TransportType, Double> kmByTransport) {

    public boolean allPriced() {
        return legs.stream().allMatch(LegQuote::isPriced);
    }
}
