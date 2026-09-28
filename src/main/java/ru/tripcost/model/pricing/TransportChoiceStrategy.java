package ru.tripcost.model.pricing;

/**
 * Критерий выбора лучшего вида транспорта.
 *
 * <p><b>Паттерн «Стратегия» (Strategy).</b> Сравнение транспорта
 * ({@link TransportComparison}) не знает, по какому правилу выбирать лучший вариант:
 * оно спрашивает оценку у стратегии. Стратегии взаимозаменяемы и выбираются
 * пользователем во время работы программы.</p>
 */
public interface TransportChoiceStrategy {

    String name();

    /** Оценка доступного варианта: чем меньше, тем лучше. */
    double score(ComparisonRow row);

    /** Пояснение к оценке для таблицы. */
    String explain(ComparisonRow row);
}
