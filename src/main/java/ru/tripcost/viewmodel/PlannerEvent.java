package ru.tripcost.viewmodel;

import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.pricing.CostBreakdown;

/**
 * События, о которых вкладки сообщают посреднику.
 */
public sealed interface PlannerEvent {

    /** Добавить участок в маршрут (из расчёта или из направлений). */
    record AddLegRequested(String from, String to, TransportType transport, String source)
            implements PlannerEvent {
    }

    /** Открыть участок маршрута на вкладке «Расчёт». */
    record OpenInCalculatorRequested(String from, String to, TransportType transport, int passengers)
            implements PlannerEvent {
    }

    /** Маршрут изменился. */
    record JourneyChanged(int legs, String total) implements PlannerEvent {
    }

    /** Оформить заявку на командировку по рассчитанной поездке. */
    record CreateRequestRequested(Trip trip, CostBreakdown cost) implements PlannerEvent {
    }

    /** Заявка сменила состояние. */
    record RequestChanged(String description) implements PlannerEvent {
    }
}
