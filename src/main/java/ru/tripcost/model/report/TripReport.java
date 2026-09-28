package ru.tripcost.model.report;

import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.pricing.CostBreakdown;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Отчёт о поездке - абстракция (Abstraction) паттерна «Мост».
 *
 * <p><b>Паттерн «Мост» (Bridge).</b> Иерархия отчётов (что показать) и иерархия
 * форматов {@link ReportFormat} (как оформить) развиваются независимо и связаны
 * ссылкой {@link #format}. Два вида отчёта и три формата дают шесть вариантов
 * без шести отдельных классов.</p>
 */
public abstract class TripReport {

    /** DecimalFormat не потокобезопасен, поэтому у каждого потока свой экземпляр. */
    private static final ThreadLocal<DecimalFormat> MONEY = ThreadLocal.withInitial(() ->
            new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(new Locale("ru", "RU"))));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /** Мост к реализации. */
    protected final ReportFormat format;
    protected final Trip trip;
    protected final CostBreakdown cost;
    private final LocalDateTime createdAt;

    protected TripReport(ReportFormat format, Trip trip, CostBreakdown cost, LocalDateTime createdAt) {
        this.format = Objects.requireNonNull(format);
        this.trip = Objects.requireNonNull(trip);
        this.cost = Objects.requireNonNull(cost);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** Готовый документ: содержание задаёт подкласс, оформление - формат. */
    public final String render() {
        String title = title();
        return format.document(title, format.title(title) + body()
                + format.note("Сформировано: " + DATE.format(createdAt)));
    }

    public ReportFormat getFormat() {
        return format;
    }

    protected abstract String title();

    protected abstract String body();

    protected String money(BigDecimal value) {
        return MONEY.get().format(value) + " " + cost.currency();
    }

    protected String route() {
        return trip.getOrigin() + " → " + trip.getDestination() + (trip.isRoundTrip() ? " и обратно" : "");
    }
}
