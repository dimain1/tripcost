package ru.tripcost.model.pricing;

import ru.tripcost.model.config.AppConfig;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;
import ru.tripcost.model.domain.TripBuilder;
import ru.tripcost.model.domain.TripValidationException;
import ru.tripcost.model.transport.TravelTimeEstimators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Сравнение одной и той же поездки на разных видах транспорта.
 *
 * <p>Для каждого вида транспорта строитель-прототип копируется
 * ({@link TripBuilder#copy()}), в копии меняется только транспорт,
 * а исходная заготовка остаётся без изменений. Время в пути оценивает
 * шаблонный метод {@link ru.tripcost.model.transport.TravelTimeEstimator}.</p>
 *
 * <p><b>Паттерн «Стратегия» (Strategy), роль Context.</b> Порядок вариантов и лучший
 * вариант определяет текущая стратегия выбора; её можно заменить в любой момент.</p>
 */
public class TransportComparison {

    private final TripCostCalculator calculator = new TripCostCalculator();
    private TransportChoiceStrategy strategy = new CheapestChoice();

    /** Стратегии, доступные пользователю. */
    public static List<TransportChoiceStrategy> strategies() {
        return List.of(new CheapestChoice(), new FastestChoice(),
                new TimeValueChoice(AppConfig.getInstance().getHourValue()));
    }

    public void setStrategy(TransportChoiceStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy);
    }

    public TransportChoiceStrategy getStrategy() {
        return strategy;
    }

    /** Варианты от лучшего к худшему по текущей стратегии; недоступные - в конце. */
    public List<ComparisonRow> compare(TripBuilder prototype) {
        List<ComparisonRow> rows = new ArrayList<>();
        for (TransportType type : TransportType.values()) {
            TripBuilder variant = prototype.copy().by(type).dropInapplicableOptions();
            if (!type.allowsTolls()) {
                variant.tolls(BigDecimal.ZERO);   // платные дороги есть только у машины
            }
            try {
                Trip trip = variant.build();
                rows.add(ComparisonRow.available(type, calculator.calculate(trip),
                        TravelTimeEstimators.forType(type).estimate(trip.getDistanceKm()),
                        trip.getLegs(), trip.getPassengers()));
            } catch (TripValidationException e) {
                rows.add(ComparisonRow.unavailable(type, e.getErrors().get(0)));
            }
        }
        rows.sort(Comparator.comparing((ComparisonRow r) -> !r.isAvailable())
                .thenComparingDouble(r -> r.isAvailable() ? strategy.score(r) : 0));
        return rows;
    }

    /** Лучший вариант по текущей стратегии. */
    public Optional<ComparisonRow> best(List<ComparisonRow> rows) {
        return rows.stream().filter(ComparisonRow::isAvailable)
                .min(Comparator.comparingDouble(strategy::score));
    }
}
