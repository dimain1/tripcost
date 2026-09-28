package ru.tripcost.model.transport;

import ru.tripcost.model.domain.CostGroup;
import ru.tripcost.model.domain.CostLeaf;
import ru.tripcost.model.domain.TransportType;
import ru.tripcost.model.domain.Trip;

import java.math.BigDecimal;

/**
 * Транспорт — продукт паттерна «Фабричный метод».
 *
 * <p>Каждый вид транспорта сам знает, как считать стоимость проезда
 * и как описать свой тариф. Клиенты работают только с этим интерфейсом.</p>
 */
public interface Transport {

    TransportType getType();

    /** Стоимость проезда по поездке (без платных дорог и проживания). */
    BigDecimal cost(Trip trip);

    /** Название основной статьи расходов: «Топливо», «Билеты» и т. п. */
    default String costLabel() {
        return "Проезд";
    }

    /**
     * Расходы на проезд в виде группы статей (паттерн «Компоновщик»).
     * Базовый транспорт даёт одну статью; декораторы добавляют свои.
     */
    default CostGroup breakdown(Trip trip) {
        return CostGroup.of("Проезд", new CostLeaf(costLabel(), cost(trip)));
    }

    /** Описание фиксированной части тарифа. */
    String describeBaseFee();

    /** Описание цены за километр. */
    String describePerKm();
}
