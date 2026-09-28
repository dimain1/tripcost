package ru.tripcost.model.report;

import ru.tripcost.model.domain.CostItem;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripOption;
import ru.tripcost.model.pricing.CostBreakdown;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Подробный отчёт: все параметры поездки и стоимость по статьям. */
public class DetailedTripReport extends TripReport {

    public DetailedTripReport(ReportFormat format, Trip trip, CostBreakdown cost, LocalDateTime createdAt) {
        super(format, trip, cost, createdAt);
    }

    @Override
    protected String title() {
        return "Расчёт стоимости поездки";
    }

    @Override
    protected String body() {
        List<ReportLine> params = new ArrayList<>();
        params.add(ReportLine.of("Откуда", trip.getOrigin()));
        params.add(ReportLine.of("Куда", trip.getDestination()));
        params.add(ReportLine.of("Расстояние в одну сторону", km(trip.getDistanceKm())));
        params.add(ReportLine.of("Туда и обратно", trip.isRoundTrip() ? "да" : "нет"));
        params.add(ReportLine.of("Транспорт", trip.getTransport().getDisplayName()));
        params.add(ReportLine.of("Пассажиров", String.valueOf(trip.getPassengers())));
        if (trip.getTransport().usesFuel()) {
            params.add(ReportLine.of("Расход топлива",
                    String.format(new Locale("ru", "RU"), "%.1f л/100 км", trip.getFuelConsumption())));
        }
        if (!trip.getOptions().isEmpty()) {
            params.add(ReportLine.of("Дополнительные услуги", trip.getOptions().stream()
                    .map(TripOption::getDisplayName).collect(Collectors.joining(", "))));
        }
        if (trip.getNights() > 0) {
            params.add(ReportLine.of("Проживание", trip.getNights() + " ноч. × " + money(trip.getPricePerNight())));
        }

        // строки стоимости - обход дерева статей расходов (Компоновщик)
        List<ReportLine> costs = new ArrayList<>();
        for (CostItem item : cost.items().children()) {
            costs.add(ReportLine.of(item.name(), money(item.amount())));
            for (CostItem child : item.children()) {
                costs.add(ReportLine.of("• " + child.name(), money(child.amount())));
            }
        }
        costs.add(ReportLine.strong("Итого", money(cost.total())));
        costs.add(ReportLine.of("На одного пассажира", money(cost.perPassenger())));

        return format.section("Поездка", params) + format.section("Стоимость", costs);
    }

    private static String km(double value) {
        return (value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value)) + " км";
    }
}
