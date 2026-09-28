package ru.tripcost.model.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Статья расходов - компонент (Component) паттерна «Компоновщик».
 *
 * <p><b>Паттерн «Компоновщик» (Composite).</b> Расходы на поездку образуют дерево:
 * группа «Проезд» состоит из билетов, багажа, страховки и скидки, группа
 * «Все расходы» - из проезда, платных дорог и проживания. Отдельная статья
 * ({@link CostLeaf}) и группа статей ({@link CostGroup}) имеют общий интерфейс,
 * поэтому сумму любой ветки можно получить одним вызовом {@link #amount()}.</p>
 */
public interface CostItem {

    String name();

    /** Сумма статьи; у группы - сумма всех вложенных статей. */
    BigDecimal amount();

    /** Вложенные статьи; у отдельной статьи список пуст. */
    List<CostItem> children();

    default boolean isGroup() {
        return false;
    }
}
